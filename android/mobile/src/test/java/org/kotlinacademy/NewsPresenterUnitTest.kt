@file:Suppress("IllegalIdentifier")

package org.kotlinacademy

import kotlinx.coroutines.experimental.Unconfined
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.kotlinacademy.common.Cancellable
import org.kotlinacademy.common.UI
import org.kotlinacademy.data.News
import org.kotlinacademy.data.NewsData
import org.kotlinacademy.presentation.news.NewsPresenter
import org.kotlinacademy.presentation.news.NewsView
import org.kotlinacademy.respositories.NewsRepository
import org.kotlinacademy.usecases.PeriodicCaller

class NewsPresenterUnitTest {

    @Before
    fun setUp() {
        UI = Unconfined
        overrideNewsRepository({ NewsData(emptyList()) })
        overridePeriodicCaller({ _, _ -> object : Cancellable {} })
    }

    @Test
    fun `Is using PeriodicCaller to refresh list since onCreate`() {
        val view = NewsView()
        var periodicCallerStarts: List<Long> = listOf()
        overridePeriodicCaller { timeMillis, callback ->
            periodicCallerStarts += timeMillis
            Cancellable()
        }
        val presenter = NewsPresenter(view)
        // When
        presenter.onCreate()
        // Then
        assertEquals(1, periodicCallerStarts.size)
        assertEquals(NewsPresenter.AUTO_REFRESH_TIME_MS, periodicCallerStarts[0])
        view.assertNoErrors()
    }

    @Test
    fun `When onCreate, loads and displays list of news`() {
        val view = NewsView()
        overrideNewsRepository { NewsData(FAKE_NEWS_LIST_1) }
        val presenter = NewsPresenter(view)
        // When
        presenter.onCreate()
        // Then
        assertEquals(FAKE_NEWS_LIST_1, view.newsList)
        view.assertNoErrors()
    }

    @Test
    fun `When onCreate, loader is displayed during repository usage but not before and after onCreate operation`() {
        val view = NewsView()

        var repositoryUsed = false
        overrideNewsRepository {
            assertTrue(view.loading)
            repositoryUsed = true
            NewsData(FAKE_NEWS_LIST_1)
        }
        val presenter = NewsPresenter(view)
        assertFalse(view.loading)
        // When
        presenter.onCreate()
        // Then
        assertTrue(repositoryUsed)
        assertFalse(view.loading)
        assertEquals(FAKE_NEWS_LIST_1, view.newsList)
        view.assertNoErrors()
    }

    @Test
    fun `When repository returns error, it is shown on view`() {
        val view = NewsView()
        overrideNewsRepository { throw NORMAL_ERROR }
        val presenter = NewsPresenter(view)
        // When
        presenter.onCreate()
        // Then
        assertNull(view.newsList)
        assertEquals(1, view.displayedErrors.size)
        assertEquals(NORMAL_ERROR, view.displayedErrors[0])
    }

    @Test
    fun `When repository returns an error, refresh displays another one`() {
        val view = NewsView()
        overrideNewsRepository { throw NORMAL_ERROR }
        val presenter = NewsPresenter(view)
        // When
        presenter.onCreate()
        presenter.onRefresh()
        // Then
        assertNull(view.newsList)
        assertEquals(2, view.displayedErrors.size)
        assertEquals(NORMAL_ERROR, view.displayedErrors[0])
        assertEquals(NORMAL_ERROR, view.displayedErrors[1])
    }

    @Test
    fun `When different data are served after refresh, they are displayed`() {
        val view = NewsView()
        val presenter = NewsPresenter(view)
        var firstRun = true
        overrideNewsRepository {
            if (firstRun) {
                firstRun = false
                NewsData(FAKE_NEWS_LIST_1)
            } else {
                NewsData(FAKE_NEWS_LIST_2)
            }
        }
        // When
        presenter.onCreate()
        presenter.onRefresh()
        // Then
        assertEquals(FAKE_NEWS_LIST_2, view.newsList)
        view.assertNoErrors()
    }

    @Test
    fun `During refresh swipeRefresh is displayed and loading is not`() {
        val view = NewsView()
        val presenter = NewsPresenter(view)
        assertFalse(view.loading)
        assertFalse(view.refresh)
        var onCreateRun = true
        var timesRepositoryUsed = 0
        overrideNewsRepository {
            timesRepositoryUsed++
            if (onCreateRun) {
                assertTrue(view.loading)
                assertFalse(view.refresh)
                onCreateRun = false
            } else {
                assertFalse(view.loading)
                assertTrue(view.refresh)
            }
            NewsData(FAKE_NEWS_LIST_1)
        }
        // When
        presenter.onCreate()
        presenter.onRefresh()
        // Then
        assertEquals(2, timesRepositoryUsed)
        assertFalse(view.loading)
        assertFalse(view.refresh)
        assertEquals(FAKE_NEWS_LIST_1, view.newsList)
        view.assertNoErrors()
    }

    @Test
    fun `News are displayed in occurrence order - from newest to oldest`() {
        val view = NewsView()
        val presenter = NewsPresenter(view)
        overrideNewsRepository { NewsData(FAKE_NEWS_LIST_2_UNSORTED) }
        // When
        presenter.onCreate()
        // Then
        assertEquals(FAKE_NEWS_LIST_2, view.newsList)
        view.assertNoErrors()
    }

    private fun overrideNewsRepository(getNewsData: () -> NewsData) {
        NewsRepository.override = object : NewsRepository {
            suspend override fun getNewsData(): NewsData = getNewsData()
        }
    }

    private fun overridePeriodicCaller(start: (timeMillis: Long, callback: () -> Unit) -> Cancellable) {
        PeriodicCaller.override = object : PeriodicCaller {
            override fun start(timeMillis: Long, callback: () -> Unit) = start(timeMillis, callback)
        }
    }

    private fun NewsView() = object : NewsView {
        override var loading: Boolean = false
        override var refresh: Boolean = false
        var newsList: List<News>? = null
        var displayedErrors: List<Throwable> = emptyList()

        override fun showList(news: List<News>) {
            newsList = news
        }

        override fun showError(throwable: Throwable) {
            displayedErrors += throwable
        }

        override fun logError(error: Throwable) {
            throw error
        }

        fun assertNoErrors() {
            displayedErrors.forEach { throw it }
            assertEquals(0, displayedErrors.size)
        }
    }

    private fun Cancellable() = object : Cancellable {}

    companion object {
        val FAKE_NEWS_1 = News(1, "Some title", "Description", "Image url", "Url", DateTime(1))
        val FAKE_NEWS_2 = News(2, "Some title 2", "Description 2", "Image url 2", "Url 2", DateTime(0))
        val FAKE_NEWS_LIST_1 = listOf(FAKE_NEWS_1)
        val FAKE_NEWS_LIST_2 = listOf(FAKE_NEWS_1, FAKE_NEWS_2)
        val FAKE_NEWS_LIST_2_UNSORTED = listOf(FAKE_NEWS_2, FAKE_NEWS_1)
        val NORMAL_ERROR = Error()
    }
}
