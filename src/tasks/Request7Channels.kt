package tasks

import contributors.GitHubService
import contributors.RequestData
import contributors.User
import contributors.log
import contributors.logRepos
import contributors.logUsers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    coroutineScope {
        val repos = service.getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .bodyList()

        val channel = Channel<List<User>>()
        repos.forEach { repo ->
            launch {
                log("starting loading for ${repo.name}")
                val users = service.getRepoContributors(req.org, repo.name)
                    .also { logUsers(repo, it) }
                    .bodyList()
                channel.send(users)
            }
        }

        // this gets "snapshotted" in Continuation
        val users = mutableListOf<User>()
        repeat(repos.size) {
            // this gets suspended ON thread X
            val nextUsers = channel.receive()
            // this is resumed ON thread Y, there not guarantee that they are the same!
            users += nextUsers
            updateResults(users.aggregate(), it == repos.lastIndex)
        }
    }
}
