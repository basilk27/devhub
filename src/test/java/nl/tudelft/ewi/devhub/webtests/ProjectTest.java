package nl.tudelft.ewi.devhub.webtests;

import nl.tudelft.ewi.devhub.server.database.controllers.Groups;
import nl.tudelft.ewi.devhub.server.database.controllers.Users;
import nl.tudelft.ewi.devhub.server.database.entities.Group;
import nl.tudelft.ewi.devhub.server.database.entities.GroupRepository;
import nl.tudelft.ewi.devhub.server.database.entities.User;
import nl.tudelft.ewi.devhub.webtests.utils.WebTest;
import nl.tudelft.ewi.devhub.webtests.views.CommitsView;
import nl.tudelft.ewi.devhub.webtests.views.CommitsView.Commit;
import nl.tudelft.ewi.devhub.webtests.views.ContributorsView;
import nl.tudelft.ewi.devhub.webtests.views.ContributorsView.Contributor;
import nl.tudelft.ewi.devhub.webtests.views.DiffElement;
import nl.tudelft.ewi.devhub.webtests.views.DiffInCommitView;
import nl.tudelft.ewi.git.models.CommitModel;
import nl.tudelft.ewi.git.models.DetailedCommitModel;
import nl.tudelft.ewi.git.models.DiffBlameModel;
import nl.tudelft.ewi.git.web.api.BranchApi;
import nl.tudelft.ewi.git.web.api.CommitApi;
import nl.tudelft.ewi.git.web.api.RepositoriesApi;
import nl.tudelft.ewi.git.web.api.RepositoryApi;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import javax.inject.Inject;

import static org.junit.Assert.*;

public class ProjectTest extends WebTest {

	@Inject Users users;
	@Inject Groups groups;
	@Inject RepositoriesApi repositoriesApi;

	private User user;
	private Group group;
	private GroupRepository groupRepository;
	private RepositoryApi repositoryApi;
	private BranchApi masterApi;
	private CommitApi commitApi;
	private DetailedCommitModel commitModel;

	@Before
	public void prepareInitialCommit() {
		user = users.findByNetId(NET_ID);
		group = groups.listFor(user).get(0);
		groupRepository = group.getRepository();
		repositoryApi = repositoriesApi.getRepository(groupRepository.getRepositoryName());
		masterApi = repositoryApi.getBranch("master");
		commitApi = masterApi.getCommit();
		commitModel = commitApi.get();
	}

	/**
	 * <h1>Opening a project overview .</h1>
	 *
	 * Given that:
	 * <ol>
	 *   <li>I am successfully logged in.</li>
	 *   <li>I have a project.</li>
	 *   <li>There is a commit in the project.</li>
	 * </ol>
	 * When:
	 * <ol>
	 *   <li>I click on a project in the project list.</li>
	 * </ol>
	 * Then:
	 * <ol>
	 *   <li>I am redirected to the project page.</li>
	 * </ol>
	 */
	@Test
	public void testListCommits() {
		CommitsView view = openLoginScreen()
				.login(NET_ID, PASSWORD)
				.toCoursesView()
				.listMyProjects()
				.get(0).click();

		List<Commit> commits = view.listCommits();
		List<CommitModel> expected = masterApi.retrieveCommitsInBranch().getCommits();
		assertEquals(expected.size(), commits.size());

		for(int i = 0, s = expected.size(); i < s; i++) {
			Commit commit = commits.get(i);
			CommitModel expectedModel = expected.get(i);
			String expectedMessage = expectedModel.getMessage();

			assertEquals(expectedModel.getAuthor(), commit.getAuthor());
			// TODO Starts with instead of equals, because tags may be included in string: v6.2.0
			assertThat(commit.getMessage(), Matchers.startsWith(expectedMessage));
		}
	}

	/**
	 * <h1>Opening a project overview .</h1>
	 *
	 * Given that:
	 * <ol>
	 *   <li>I am successfully logged in.</li>
	 *   <li>I have a project.</li>
	 *   <li>There is a commit in the project.</li>
	 * </ol>
	 * When:
	 * <ol>
	 *   <li>I click on a project in the project list.</li>
	 * </ol>
	 * Then:
	 * <ol>
	 *   <li>I am redirected to the diff page.</li>
	 * </ol>
	 */
	@Test
	@Ignore("No easy way to produce empty diff right now")
	public void testViewCommitDiffEmpty() {
		DiffInCommitView view = openLoginScreen()
				.login(NET_ID, PASSWORD)
				.toCoursesView()
				.listMyProjects()
				.get(0).click()
				.listCommits()
				.get(0).click();

		List<DiffElement> list = view.listDiffs();
		assertEquals(commitModel.getAuthor(), view.getAuthorHeader());
		assertEquals(commitModel.getMessage(), view.getMessageHeader());
		assertTrue("Expected empty list", list.isEmpty());
	}

	/**
	 * <h1>Opening a project overview .</h1>
	 *
	 * Given that:
	 * <ol>
	 *   <li>I am successfully logged in.</li>
	 *   <li>I have a project.</li>
	 *   <li>There is a commit in the project.</li>
	 * </ol>
	 * When:
	 * <ol>
	 *   <li>I click on a project in the project list.</li>
	 * </ol>
	 * Then:
	 * <ol>
	 *   <li>I am redirected to the diff page.</li>
	 * </ol>
	 */
	@Test
	public void testViewCommitDiff() {

		DiffBlameModel diffBlameModel = commitApi.diffBlame();

		DiffInCommitView view = openLoginScreen()
				.login(NET_ID, PASSWORD)
				.toCoursesView()
				.listMyProjects()
				.get(0).click()
				.listCommits()
				.get(0).click();

		assertEquals(commitModel.getAuthor(), view.getAuthorHeader());
		assertEquals("+" + diffBlameModel.getLinesAdded(), view.getLinesAdded());
		assertEquals("/", view.getNeutralLines());
		assertEquals("-"+ diffBlameModel.getLinesRemoved(), view.getLinesRemoved());
		assertEquals(commitModel.getMessage(), view.getMessageHeader());

		List<DiffElement> list = view.listDiffs();
		DiffElement result = list.get(0);
		result.assertEqualTo(diffBlameModel.getDiffs().get(0));
	}

	@Test
	public void testListContributors() {
		ContributorsView view = openLoginScreen()
			.login(NET_ID, PASSWORD)
			.toCoursesView()
			.listMyProjects()
			.get(0).click()
			.toContributorsView();

		List<Contributor> contributors = view.listContributors();
		assertEquals(2, contributors.size());

		for(int i = 0, s = 2; i < s; i++) {
			Contributor assignment = contributors.get(i);
			if(i==0){
				assertEquals(assignment.getNetID(), "student1");
				assertEquals(assignment.getName(), "Student One");
				assertEquals(assignment.getEmail(), "student-1@student.tudelft.nl");
			}
			if(i==1){
				assertEquals(assignment.getNetID(), "student2");
				assertEquals(assignment.getName(), "Student Two");
				assertEquals(assignment.getEmail(), "student-2@student.tudelft.nl");
			}
		}
	}

	@Test
	public void testEmptyCommentPreview() {
		DiffInCommitView view = openLoginScreen()
				.login(NET_ID, PASSWORD)
				.toCoursesView()
				.listMyProjects()
				.get(0).click()
				.listCommits()
				.get(0).click();

		view.setCommentInput("");
		view.renderPreview();
		assertEquals("" ,view.getPreviewPanelContent());
	}

	@Test
	public void testSmallCommentPreview() {
		DiffInCommitView view = openLoginScreen()
				.login(NET_ID, PASSWORD)
				.toCoursesView()
				.listMyProjects()
				.get(0).click()
				.listCommits()
				.get(0).click();

		view.setCommentInput("Hello World!");
		view.renderPreview();
		assertEquals("Hello World!", view.getPreviewPanelContent());
	}
}
