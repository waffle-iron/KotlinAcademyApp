package org.kotlinacademy.desktop.views

import org.kotlinacademy.data.News
import org.kotlinacademy.desktop.Styles
import org.kotlinacademy.presentation.news.NewsPresenter
import org.kotlinacademy.presentation.news.NewsView
import tornadofx.*


class TornadoNewsView : BaseTornadoView("Kotlin Academy"), NewsView {
    private val loadingProperty = SimpleBooleanProperty()
    override var loading by loadingProperty

    private val refreshProperty = SimpleBooleanProperty()
    override var refresh by refreshProperty

    private val newsList = observableArrayList<News>()!!
    private val presenter = NewsPresenter(this)

    override val root = borderpane {
        addClass(Styles.newsView)
        top {
            stackpane {
                addClass(Styles.header)
                vbox {
                    addClass(Styles.listCenter)
                    label(title).addClass(Styles.headerTitle)
                    label("With mission to simplify Kotlin learning")
                }
                button {
                    enableWhen(refreshProperty.not())
                    graphic = svgicon(Styles.refreshIcon, 20, Color.WHITE)
                    stackpaneConstraints { alignment = Pos.TOP_LEFT }
                    addClass(Styles.icon)
                    action(presenter::onRefresh)
                    rotateWhenRefreshing()
                    tooltip("Click to refresh the news list")
                }
            }
        }
        center {
            stackpane {
                listview(newsList) {
                    removeWhen(loadingProperty)
                    addClass(Styles.newsList)
                    cellFragment(NewsListFragment::class)
                    onUserSelect { hostServices.showDocument(it.url) }
                }
                progressbar {
                    removeWhen(loadingProperty.not())
                }
                button {
                    graphic = svgicon(Styles.replyIcon)
                    addClass(Styles.icon, Styles.generalFeedback)
                    stackpaneConstraints { alignment = BOTTOM_RIGHT }
                    tooltip("Click to leave a comment")
                    action { find<CommentForm>(Scope()).openModal() }
                }
            }
        }
    }

    private fun Button.rotateWhenRefreshing() {
        val rotation = rotate(0.3.seconds, 360, Interpolator.LINEAR, play = false) {
            cycleCount = Timeline.INDEFINITE
            isAutoReverse = true
        }

        refreshProperty.onChange {
            if (it) {
                rotation.playFromStart()
            } else {
                rotation.stop()
                rotate = 0.0
            }
        }
    }

    override fun onDock() {
        presenter.onCreate()
    }

    override fun showList(news: List<News>) {
        newsList.setAll(news)
    }
}