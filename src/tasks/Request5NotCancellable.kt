package tasks

import contributors.*
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

@OptIn(DelicateCoroutinesApi::class)
suspend fun loadContributorsNotCancellable(service: GitHubService, req: RequestData): List<User> {
    val repos = service.getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    return repos.map { repo ->
        // spawn independent coroutines in global scope, now cancellation of 'loading' coroutines doesn't affect
        // these coroutines at all
        GlobalScope.async(Dispatchers.Default) {
            log("starting loading for ${repo.name}")
            delay(3000)
            service.getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }.awaitAll()
        .flatten()
        .aggregate()
}