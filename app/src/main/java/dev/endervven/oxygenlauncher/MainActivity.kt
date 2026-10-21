package dev.endervven.oxygenlauncher

import android.app.Activity
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class AppEntry(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var content: FrameLayout
    private lateinit var appHost: AppWidgetHost
    private val widgetHostId = 417
    private var drawerOpen = false
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var currentQuery = ""
    private var allApps: List<AppEntry> = emptyList()
    private var page = 0
    private val homePages = 3
    private val widgetRequest = 900

    private val bg get() = if (LauncherPrefs.dark(this)) Color.rgb(17, 19, 23) else Color.rgb(247, 248, 251)
    private val surface get() = if (LauncherPrefs.dark(this)) Color.rgb(29, 32, 38) else Color.WHITE
    private val foreground get() = if (LauncherPrefs.dark(this)) Color.rgb(245, 246, 250) else Color.rgb(26, 29, 36)
    private val secondary get() = if (LauncherPrefs.dark(this)) Color.rgb(164, 170, 182) else Color.rgb(108, 115, 128)
    private val accent get() = LauncherPrefs.accent(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = if (!LauncherPrefs.dark(this)) {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else 0
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        appHost = AppWidgetHost(this, widgetHostId)
        appHost.startListening()
        loadApps()
        showHome()
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) {
            window.statusBarColor = bg
            window.navigationBarColor = bg
            loadApps()
            if (!drawerOpen) showHome()
        }
    }

    private fun loadApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        allApps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { ri ->
                val ai = ri.activityInfo ?: return@mapNotNull null
                if (ai.packageName == packageName) return@mapNotNull null
                AppEntry(ri.loadLabel(pm).toString(), ai.packageName, ri.loadIcon(pm))
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun rounded(color: Int, radius: Int = 24, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun baseLayout(): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            clipToPadding = false
            setPadding(dp(20), dp(14), dp(20), dp(8))
        }

        content = FrameLayout(this)
        setContentView(root)

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(
                    android.view.WindowInsets.Type.systemBars()
                )
                view.setPadding(
                    dp(20),
                    bars.top + dp(14),
                    dp(20),
                    bars.bottom + dp(8)
                )
                insets
            }
            root.requestApplyInsets()
        }

        return root
    }

    private fun showHome() {
        drawerOpen = false
        baseLayout()
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val date = TextView(this).apply {
            text = SimpleDateFormat("EEEE, d. MMMM", Locale.getDefault()).format(Date())
                .replaceFirstChar { it.uppercase() }
            textSize = 13f
            setTextColor(this@MainActivity.secondary)
        }
        val settings = TextView(this).apply {
            text = "⚙"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(this@MainActivity.foreground)
            background = rounded(surface, 18)
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
            contentDescription = "Einstellungen"
        }
        top.addView(date, LinearLayout.LayoutParams(0, dp(46), 1f))
        top.addView(settings, LinearLayout.LayoutParams(dp(46), dp(46)))
        root.addView(top)

        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(18), 0, dp(12))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val clock = TextView(this).apply {
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            textSize = 64f
            letterSpacing = -0.045f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(this@MainActivity.foreground)
        }
        body.addView(clock)
        val subtitle = TextView(this).apply {
            text = "Einfach. Flüssig. Deins."
            textSize = 15f
            setTextColor(this@MainActivity.secondary)
        }
        body.addView(subtitle)

        val search = EditText(this).apply {
            hint = "Apps suchen"
            textSize = 15f
            isSingleLine = true
            setTextColor(this@MainActivity.foreground)
            setHintTextColor(secondary)
            background = rounded(surface, 22)
            compoundDrawablePadding = dp(10)
            setPadding(dp(16), 0, dp(16), 0)
            setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0)
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDrawer("") }
            setOnClickListener { showDrawer("") }
        }
        val searchLp = LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(24); bottomMargin = dp(20) }
        body.addView(search, searchLp)

        val pageRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val pageTitle = TextView(this).apply {
            text = when (page) { 1 -> "Deine Favoriten"; 2 -> "Widgets & Tools"; else -> "Deine Apps" }
            textSize = 19f; typeface = Typeface.DEFAULT_BOLD; setTextColor(this@MainActivity.foreground)
        }
        val pageHint = TextView(this).apply {
            text = "  ${page + 1}/$homePages  ›"
            textSize = 13f; setTextColor(this@MainActivity.secondary)
            setOnClickListener { page = (page + 1) % homePages; showHome() }
        }
        pageRow.addView(pageTitle)
        pageRow.addView(pageHint)
        body.addView(pageRow)

        when (page) {
            0 -> addAppGrid(body, allApps.take(8), LauncherPrefs.grid(this))
            1 -> addFavorites(body)
            else -> addWidgetPanel(body)
        }

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(surface, 28)
            setPadding(dp(6), dp(8), dp(6), dp(8))
        }
        val dockApps = listOfNotNull(
            findApp("com.google.android.dialer", "com.android.dialer"),
            findApp("com.google.android.apps.messaging", "com.android.messaging"),
            findApp("com.android.chrome"),
            findApp("com.google.android.googlequicksearchbox")
        )
        if (dockApps.isEmpty()) {
            listOf("Apps", "Widgets", "Einstellungen").forEachIndexed { i, label ->
                dock.addView(dockItem(label, "◉") {
                    when (i) { 0 -> showDrawer(""); 1 -> { page = 2; showHome() }; else -> startActivity(Intent(this, SettingsActivity::class.java)) }
                }, LinearLayout.LayoutParams(0, dp(62), 1f))
            }
        } else {
            dockApps.forEach { app -> dock.addView(dockItem(app.label, app.icon) { launch(app) }, LinearLayout.LayoutParams(0, dp(62), 1f)) }
            dock.addView(dockItem("Alle", "⠿") { showDrawer("") }, LinearLayout.LayoutParams(0, dp(62), 1f))
        }
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(78)).apply { topMargin = dp(8) })

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { touchStartX = event.x; touchStartY = event.y; true }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - touchStartX
                    val dy = event.y - touchStartY
                    if (abs(dx) > dp(65) && abs(dx) > abs(dy)) {
                        page = (page + if (dx < 0) 1 else homePages - 1) % homePages
                        animatePage()
                        showHome()
                    } else if (dy < -dp(70)) showDrawer("")
                    else if (dy > dp(90)) showDrawer("")
                    true
                }
                else -> true
            }
        }
        animatePage()
    }

    private fun animatePage() {
        if (LauncherPrefs.animations(this) && ::root.isInitialized) {
            root.alpha = 0.88f
            root.translationY = dp(8).toFloat()
            root.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun addAppGrid(parent: LinearLayout, apps: List<AppEntry>, columns: Int) {
        if (apps.isEmpty()) {
            val empty = TextView(this).apply { text = "Noch keine Apps gefunden."; setTextColor(this@MainActivity.secondary); setPadding(0, dp(20), 0, dp(20)) }
            parent.addView(empty); return
        }
        val colCount = columns.coerceIn(3, 6)
        apps.chunked(colCount).forEach { rowApps ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP }
            rowApps.forEach { app ->
                row.addView(appTile(app), LinearLayout.LayoutParams(0, dp(104), 1f))
            }
            repeat(colCount - rowApps.size) { row.addView(View(this), LinearLayout.LayoutParams(0, dp(104), 1f)) }
            parent.addView(row)
        }
    }

    private fun appTile(app: AppEntry): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(2), dp(8), dp(2), dp(8))
            setOnClickListener { launch(app) }
            setOnLongClickListener {
                val options = arrayOf("App öffnen", "App-Info", "Deinstallieren")
                android.app.AlertDialog.Builder(this@MainActivity).setTitle(app.label).setItems(options) { _, which ->
                    when (which) {
                        0 -> launch(app)
                        1 -> startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
                        2 -> startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
                    }
                }.show()
                true
            }
        }
        val icon = ImageView(this).apply {
            setImageDrawable(app.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
            if (LauncherPrefs.animations(this@MainActivity)) {
                setOnTouchListener { v, e ->
                    if (e.action == MotionEvent.ACTION_DOWN) v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(90).start()
                    if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    false
                }
            }
        }
        tile.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
        if (LauncherPrefs.labels(this)) {
            val label = TextView(this).apply {
                text = app.label; textSize = 11.5f; gravity = Gravity.CENTER
                setTextColor(this@MainActivity.foreground); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            }
            tile.addView(label, LinearLayout.LayoutParams(-1, dp(28)))
        }
        return tile
    }

    private fun dockItem(label: String, icon: Any, click: () -> Unit): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setOnClickListener { click() } }
        if (icon is android.graphics.drawable.Drawable) {
            box.addView(ImageView(this).apply { setImageDrawable(icon) }, LinearLayout.LayoutParams(dp(30), dp(30)))
        } else {
            box.addView(TextView(this).apply { text = icon.toString(); textSize = 25f; gravity = Gravity.CENTER; setTextColor(accent) }, LinearLayout.LayoutParams(dp(30), dp(30)))
        }
        if (LauncherPrefs.labels(this)) box.addView(TextView(this).apply { text = label; textSize = 10f; setTextColor(this@MainActivity.secondary); gravity = Gravity.CENTER })
        return box
    }

    private fun findApp(vararg packages: String): AppEntry? = packages.firstNotNullOfOrNull { p -> allApps.firstOrNull { it.packageName == p } }

    private fun addFavorites(parent: LinearLayout) {
        val favs = LauncherPrefs.prefs(this).getStringSet("favorites", emptySet()) ?: emptySet()
        val apps = allApps.filter { it.packageName in favs }.ifEmpty { allApps.take(8) }
        addAppGrid(parent, apps.take(16), LauncherPrefs.grid(this))
        val note = TextView(this).apply {
            text = if (favs.isEmpty()) "Tipp: Halte ein App-Icon gedrückt, um Optionen zu öffnen. Favoriten lassen sich in den Einstellungen verwalten." else "Deine gespeicherten Favoriten"
            textSize = 13f; setTextColor(this@MainActivity.secondary); setPadding(0, dp(10), 0, 0)
        }
        parent.addView(note)
    }

    private fun addWidgetPanel(parent: LinearLayout) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 26)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        card.addView(TextView(this).apply { text = "Widgets"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(this@MainActivity.foreground) })
        card.addView(TextView(this).apply {
            text = "Füge Android-Widgets über die Schaltfläche hinzu. Die Widget-Auswahl wird vom System bereitgestellt."
            textSize = 13f; setTextColor(this@MainActivity.secondary); setPadding(0, dp(8), 0, dp(16))
        })
        val add = Button(this).apply {
            text = "＋ Widget hinzufügen"
            setTextColor(Color.WHITE)
            background = rounded(accent, 18)
            setOnClickListener { pickWidget() }
        }
        card.addView(add, LinearLayout.LayoutParams(-1, dp(48)))
        val wallpaper = Button(this).apply {
            text = "Wallpaper ändern"
            setOnClickListener {
                try { startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)) }
                catch (_: Exception) { startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) }
            }
        }
        card.addView(wallpaper, LinearLayout.LayoutParams(-1, dp(48)))
        parent.addView(card)
    }

    private fun pickWidget() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val provider = appWidgetManager.installedProviders.firstOrNull()
            if (provider == null) {
                Toast.makeText(this, "Keine Widget-Anbieter verfügbar.", Toast.LENGTH_SHORT).show()
                return
            }
            val id = appHost.allocateAppWidgetId()
            val bind = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            startActivityForResult(bind, widgetRequest)
        } catch (e: Exception) {
            Toast.makeText(this, "Widget-Auswahl ist auf dieser Android-Version eingeschränkt.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == widgetRequest) {
            Toast.makeText(this, "Widget-Bindung angefragt. Vollständiger Widget-Picker hängt vom Gerätehersteller ab.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDrawer(initial: String) {
        drawerOpen = true
        baseLayout()
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = "Alle Apps"; textSize = 26f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); setTextColor(this@MainActivity.foreground)
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        header.addView(TextView(this).apply {
            text = "✕"; textSize = 22f; gravity = Gravity.CENTER; setTextColor(this@MainActivity.foreground)
            setOnClickListener { showHome() }
        }, LinearLayout.LayoutParams(dp(44), dp(52)))
        root.addView(header)
        val search = EditText(this).apply {
            hint = "Suchen…"; textSize = 15f; isSingleLine = true
            setTextColor(this@MainActivity.foreground); setHintTextColor(secondary); background = rounded(surface, 20)
            setPadding(dp(16), 0, dp(16), 0)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8); bottomMargin = dp(12) })
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        fun render(query: String) {
            list.removeAllViews()
            val filtered = allApps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
            val cols = LauncherPrefs.grid(this).coerceIn(3, 6)
            filtered.chunked(cols).forEach { apps ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                apps.forEach { app -> row.addView(appTile(app), LinearLayout.LayoutParams(0, dp(104), 1f)) }
                repeat(cols - apps.size) { row.addView(View(this), LinearLayout.LayoutParams(0, dp(104), 1f)) }
                list.addView(row)
            }
            if (filtered.isEmpty()) list.addView(TextView(this).apply { text = "Keine Treffer"; setTextColor(this@MainActivity.secondary); setPadding(0, dp(20), 0, 0) })
        }
        search.setText(initial)
        render(initial)
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        search.requestFocus()
        if (LauncherPrefs.animations(this)) {
            root.alpha = 0f
            root.animate().alpha(1f).setDuration(180).setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun launch(app: AppEntry) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "App konnte nicht gestartet werden.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (drawerOpen) showHome() else super.onBackPressed()
    }

    override fun onDestroy() {
        try { appHost.stopListening() } catch (_: Exception) {}
        super.onDestroy()
    }
}
