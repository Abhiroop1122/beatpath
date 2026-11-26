package com.beatpath.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.audio.Sfx
import com.beatpath.app.data.EarIds
import com.beatpath.app.data.Store
import com.beatpath.app.lessons.Curriculum
import com.beatpath.app.lessons.Practice
import com.beatpath.app.model.Project
import com.beatpath.app.ui.home.HomeScaffold
import com.beatpath.app.ui.home.LearnPath
import com.beatpath.app.ui.home.LoopsScreen
import com.beatpath.app.ui.home.MeScreen
import com.beatpath.app.ui.home.PracticeScreen
import com.beatpath.app.ui.home.StudioHub
import com.beatpath.app.ui.home.Tab
import com.beatpath.app.ui.home.WelcomeScreen
import com.beatpath.app.ui.lesson.EarLessonScreen
import com.beatpath.app.ui.lesson.EarLessons
import com.beatpath.app.ui.lesson.LessonCompleteScreen
import com.beatpath.app.ui.lou.LouGallery
import com.beatpath.app.ui.studio.StudioScreen
import com.beatpath.app.ui.theme.BeatpathTheme

sealed interface Route {
    data object Welcome : Route
    data class Home(val tab: Tab) : Route
    /** [from] is the tab you came from, so leaving always lands you back where you were. */
    data class Ear(val id: String, val from: Tab = Tab.Learn, val run: Long = System.nanoTime()) : Route
    data class Studio(val project: Project, val lessonId: String? = null, val from: Tab = Tab.Learn, val run: Long = System.nanoTime()) : Route
    data class Complete(val lessonId: String, val xp: Int, val unassisted: Boolean, val seconds: Int, val from: Tab = Tab.Learn) : Route
}

class MainActivity : ComponentActivity() {
    private val engine = AudioEngine()
    private val sfx by lazy { Sfx(engine) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store(applicationContext)
        com.beatpath.app.lessons.Practice.accuracy = { store.accuracy(it.id) }
        val debuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable && intent?.getBooleanExtra("debug_complete_all", false) == true) store.debugCompleteAll()
        engine.sfxEnabled = store.sfxOn
        val gallery = intent?.getBooleanExtra("lou_gallery", false) == true
        val startRoute = intent?.getStringExtra("route")
        setContent {
            if (gallery) { LouGallery(); return@setContent }
            BeatpathTheme {
                var route by remember {
                    mutableStateOf<Route>(when {
                        startRoute != null && (Curriculum.byId(startRoute) != null || startRoute == Practice.DICTATION) -> Route.Studio(Project(1, "Debug"), startRoute)
                        startRoute != null && (startRoute in EarIds.all || Practice.isEar(startRoute)) -> Route.Ear(startRoute)
                        startRoute == "practice" -> Route.Home(Tab.Practice)
                        startRoute == "home" -> Route.Home(Tab.Learn)
                        startRoute == "welcome" -> Route.Welcome
                        startRoute == "complete" -> Route.Complete("debug", 15, true, 74)
                        startRoute == "studio" -> Route.Studio(Project(System.currentTimeMillis(), "Untitled loop"), from = Tab.Studio)
                        store.onboarded -> Route.Home(Tab.Learn)
                        else -> Route.Welcome
                    })
                }
                fun start(id: String, from: Tab = Tab.Learn) {
                    route = if (id in EarIds.all || Practice.isEar(id)) Route.Ear(id, from)
                    else Route.Studio(Project(System.currentTimeMillis(), Curriculum.byId(id)?.title ?: if (id == Practice.DICTATION) "Dictation" else "Lesson"), id, from)
                }
                // system Back: other tabs → Learn tab; Lesson complete → where you came from (Learn otherwise exits the app)
                androidx.activity.compose.BackHandler(enabled = (route as? Route.Home)?.tab?.let { it != Tab.Learn } == true) { route = Route.Home(Tab.Learn) }
                androidx.activity.compose.BackHandler(enabled = route is Route.Complete) { route = Route.Home((route as Route.Complete).from) }
                AnimatedContent(route, transitionSpec = {
                    // Apple-like: content eases in with a soft spring scale while the old screen fades away
                    (fadeIn(androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f))) +
                        androidx.compose.animation.scaleIn(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 380f), initialScale = 0.96f)) togetherWith
                        (fadeOut(androidx.compose.animation.core.tween(200)) + androidx.compose.animation.scaleOut(androidx.compose.animation.core.tween(220), targetScale = 1.015f))
                }, contentKey = { it::class }, label = "route") { r ->
                    when (r) {
                        Route.Welcome -> WelcomeScreen(sfx,
                            onStart = { store.finishOnboarding(false); route = Route.Ear(EarIds.HIGH_LOW) },
                            onJump = { store.finishOnboarding(true); route = Route.Home(Tab.Learn) })
                        is Route.Home -> HomeScaffold(r.tab, sfx, onTab = { route = Route.Home(it) }) {
                            when (r.tab) {
                                Tab.Learn -> LearnPath(store, sfx, onPractice = { route = Route.Home(Tab.Practice) },
                                    onSong = { route = Route.Studio(Project(System.currentTimeMillis(), "My song", pattern = com.beatpath.app.model.Pattern(4)), from = Tab.Learn) }) { start(it, Tab.Learn) }
                                Tab.Practice -> PracticeScreen(store, sfx) { start(it, Tab.Practice) }
                                Tab.Studio -> StudioHub(store, sfx, onFree = { route = Route.Studio(Project(System.currentTimeMillis(), "Untitled loop"), from = Tab.Studio) }, onStart = { start(it, Tab.Studio) })
                                Tab.Loops -> LoopsScreen(store, engine, sfx, onOpen = { route = Route.Studio(it, from = Tab.Loops) }, onNew = { route = Route.Studio(Project(System.currentTimeMillis(), "Untitled loop"), from = Tab.Loops) })
                                Tab.Me -> MeScreen(store, engine)
                            }
                        }
                        is Route.Ear -> EarLessonScreen(remember(r.run) { EarLessons.spec(r.id) }, engine, sfx,
                            onExit = { route = Route.Home(r.from) },
                            onFinished = { un, attempt, secs ->
                                sfx.complete()
                                val xp = if (Practice.isPractice(r.id)) store.recordPractice(attempt, if (un) 10 else 8) else store.recordLesson(r.id, attempt, un)
                                route = Route.Complete(r.id, xp, un, secs, r.from)
                            },
                            onAnswer = { kind, ok -> store.recordSkill(Practice.skillOf(kind).id, ok) })
                        is Route.Studio -> StudioScreen(engine, sfx, r.project, Curriculum.byId(r.lessonId) ?: remember(r.run) { Practice.lesson(r.lessonId, r.run) },
                            onSave = { store.saveProject(it) },
                            onExit = { route = Route.Home(r.from) },
                            onLessonDone = { id, attempt, un ->
                                val xp = if (Practice.isPractice(id)) { store.recordSkill(Practice.Skill.DICTATION.id, un); store.recordPractice(attempt, if (un) 14 else 10) }
                                         else store.recordLesson(id, attempt, un)
                                route = Route.Complete(id, xp, un, 0, r.from)
                            })
                        is Route.Complete -> LessonCompleteScreen(sfx, r.xp, r.unassisted, r.seconds,
                            onContinue = { route = Route.Home(r.from) }, onAgain = { start(r.lessonId, r.from) })
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); engine.start() }
    override fun onPause() { engine.stop(); engine.release(); super.onPause() }
}
