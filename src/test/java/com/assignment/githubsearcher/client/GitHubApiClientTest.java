package com.assignment.githubsearcher.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.assignment.githubsearcher.exception.GitHubApiException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GitHubApiClientTest {

    private MockRestServiceServer server;
    private GitHubApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GitHubApiClient(builder, "http://localhost", "test-token");
    }

    @Test
    void searchRepositories_mapsSuccessfulResponse() {
        server.expect(requestTo("http://localhost/search/repositories?q=spring%20boot%20language:Java&sort=stars&order=desc&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            {
                              "id": 1,
                              "name": "demo",
                              "description": "desc",
                              "owner": { "login": "octocat" },
                              "language": "Java",
                              "stargazers_count": 10,
                              "forks_count": 2,
                              "updated_at": "2024-01-01T00:00:00Z"
                            },
                            null,
                            {
                              "id": null,
                              "name": "skip-me"
                            },
                            {
                              "id": 2,
                              "name": "no-owner",
                              "description": null,
                              "owner": null,
                              "language": "Java",
                              "stargazers_count": null,
                              "forks_count": null,
                              "updated_at": "2024-02-01T00:00:00Z"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<GitHubSearchResult> results = client.searchRepositories("spring boot", "Java", "stars");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).ownerLogin()).isEqualTo("octocat");
        assertThat(results.get(1).ownerLogin()).isEqualTo("unknown");
        assertThat(results.get(1).stars()).isZero();
        assertThat(results.get(1).forks()).isZero();
        server.verify();
    }

    @Test
    void searchRepositories_defaultsSortAndSkipsLanguage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer localServer = MockRestServiceServer.bindTo(builder).build();
        GitHubApiClient noTokenClient = new GitHubApiClient(builder, "http://localhost", "");

        localServer.expect(requestTo("http://localhost/search/repositories?q=react&sort=stars&order=desc&per_page=30"))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        assertThat(noTokenClient.searchRepositories("react", "  ", null)).isEmpty();
        localServer.verify();
    }

    @Test
    void searchRepositories_returnsEmptyWhenBodyNullItems() {
        server.expect(requestTo("http://localhost/search/repositories?q=x&sort=forks&order=desc&per_page=30"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(client.searchRepositories("x", null, "forks")).isEmpty();
    }

    @Test
    void searchRepositories_returnsEmptyWhenResponseBodyIsNull() {
        server.expect(requestTo("http://localhost/search/repositories?q=x&sort=stars&order=desc&per_page=30"))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        assertThat(client.searchRepositories("x", null, "stars")).isEmpty();
    }

    @Test
    void searchRepositories_mapsRateLimit() {
        server.expect(requestTo("http://localhost/search/repositories?q=x&sort=stars&order=desc&per_page=30"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("rate limit").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.searchRepositories("x", null, "stars"))
                .isInstanceOf(GitHubApiException.class)
                .extracting(ex -> ((GitHubApiException) ex).getCode())
                .isEqualTo("GITHUB_RATE_LIMIT");
    }

    @Test
    void searchRepositories_mapsTooManyRequests() {
        server.expect(requestTo("http://localhost/search/repositories?q=x&sort=stars&order=desc&per_page=30"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.searchRepositories("x", null, "stars"))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(ex -> assertThat(((GitHubApiException) ex).getHttpStatus()).isEqualTo(429));
    }

    @Test
    void searchRepositories_mapsValidationError() {
        server.expect(requestTo("http://localhost/search/repositories?q=x&sort=stars&order=desc&per_page=30"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> client.searchRepositories("x", null, "stars"))
                .isInstanceOf(GitHubApiException.class)
                .extracting(ex -> ((GitHubApiException) ex).getCode())
                .isEqualTo("GITHUB_VALIDATION_ERROR");
    }

    @Test
    void searchRepositories_mapsServerErrorAsBadGateway() {
        server.expect(requestTo("http://localhost/search/repositories?q=x&sort=stars&order=desc&per_page=30"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("boom"));

        assertThatThrownBy(() -> client.searchRepositories("x", null, "stars"))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(ex -> {
                    GitHubApiException apiEx = (GitHubApiException) ex;
                    assertThat(apiEx.getHttpStatus()).isEqualTo(502);
                    assertThat(apiEx.getCode()).isEqualTo("GITHUB_API_ERROR");
                });
    }

    @Test
    void searchRepositories_mapsClientErrorStatus() {
        server.expect(requestTo("http://localhost/search/repositories?q=x&sort=stars&order=desc&per_page=30"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("missing"));

        assertThatThrownBy(() -> client.searchRepositories("x", null, "stars"))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(ex -> assertThat(((GitHubApiException) ex).getHttpStatus()).isEqualTo(404));
    }

    @Test
    void searchRepositories_wrapsUnexpectedFailures() {
        RestClient.Builder builder = RestClient.builder();
        GitHubApiClient brokenClient = new GitHubApiClient(builder, "http://127.0.0.1:9", null);

        assertThatThrownBy(() -> brokenClient.searchRepositories("x", null, "stars"))
                .isInstanceOf(GitHubApiException.class)
                .extracting(ex -> ((GitHubApiException) ex).getCode())
                .isEqualTo("GITHUB_API_ERROR");
    }
}
