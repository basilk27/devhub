package nl.tudelft.ewi.devhub.server.web.resources;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.devhub.server.backend.AssignmentStats;
import nl.tudelft.ewi.devhub.server.backend.DeliveriesBackend;
import nl.tudelft.ewi.devhub.server.backend.mail.ReviewMailer;
import nl.tudelft.ewi.devhub.server.database.controllers.AssignedTAs;
import nl.tudelft.ewi.devhub.server.database.controllers.Assignments;
import nl.tudelft.ewi.devhub.server.database.controllers.CourseEditions;
import nl.tudelft.ewi.devhub.server.database.controllers.Deliveries;
import nl.tudelft.ewi.devhub.server.database.entities.*;
import nl.tudelft.ewi.devhub.server.database.entities.Delivery.Review;
import nl.tudelft.ewi.devhub.server.database.entities.Delivery.State;
import nl.tudelft.ewi.devhub.server.database.entities.rubrics.Characteristic;
import nl.tudelft.ewi.devhub.server.database.entities.rubrics.GradingStrategy;
import nl.tudelft.ewi.devhub.server.database.entities.rubrics.Mastery;
import nl.tudelft.ewi.devhub.server.database.entities.rubrics.Task;
import nl.tudelft.ewi.devhub.server.web.errors.UnauthorizedException;
import nl.tudelft.ewi.devhub.server.web.templating.TemplateEngine;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jboss.resteasy.spi.NotImplementedYetException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static nl.tudelft.ewi.devhub.server.database.controllers.AssignedTAs.assignGroups;


/**
 * Created by jgmeligmeyling on 04/03/15.
 * @author Jan-Willem Gmleig Meyling
 */
@Slf4j
@Path("courses/{courseCode}/{editionCode}/assignments")
@Produces(MediaType.TEXT_HTML + Resource.UTF8_CHARSET)
public class AssignmentsResource extends Resource {

    private static final String DATE_FORMAT = "dd-MM-yyyy HH:mm";
    public static final String CHECKBOX_CHECKED_VALUE = "on";

    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private CourseEditions courses;

    @Inject
    private Assignments assignmentsDAO;

    @Inject
    private DeliveriesBackend deliveriesBackend;

    @Inject
    private Deliveries deliveriesDAO;

    @Inject
    private ReviewMailer reviewMailer;

    @Inject
    @Named("current.user")
    private User currentUser;

    @Inject
    private AssignedTAs assignedTAs;

	@Context
	HttpServletRequest request;

	@Context
	HttpServletResponse response;

    /**
     * Get an overview of the courses
     * @param courseCode the course to create an assignment for
     * @param editionCode the course to create an assignment for
     * @return a Response containing the generated page
     */
    @GET
    public Response getOverviewPage(@PathParam("courseCode") String courseCode,
									@PathParam("editionCode") String editionCode) {
        throw new NotImplementedYetException();
    }

    /**
     * Present the user a form to create a new assignment
     * @param courseCode the course to create an assignment for
	 * @param editionCode the course to create an assignment for
     * @return a Response containing the generated page
     */
    @GET
    @Transactional
    @Path("create")
    public Response getCreatePage(@PathParam("courseCode") String courseCode,
								  @PathParam("editionCode") String editionCode,
                                  @QueryParam("error") String error) throws IOException {

        CourseEdition course = courses.find(courseCode, editionCode);
        if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
            throw new UnauthorizedException();
        }

		Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("user", currentUser);
        parameters.put("course", course);

        if(error != null)
            parameters.put("error", error);

        List<Locale> locales = Collections.list(request.getLocales());
        return display(templateEngine.process("courses/assignments/create-assignment.ftl", locales, parameters));
    }


    @GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("create/bliep")
	public List<CopyableCourseEdition> otherAssignmentsWithRubrics(@PathParam("courseCode") String courseCode,
														  @PathParam("editionCode") String editionCode ) {
		CourseEdition courseEdition = courses.find(courseCode, editionCode);

		if(!(currentUser.isAdmin() || currentUser.isAssisting(courseEdition))) {
			throw new UnauthorizedException();
		}

		return copyableCourseEditionsFromCourse(courseEdition.getCourse());
	}

	private static List<CopyableCourseEdition> copyableCourseEditionsFromCourse(Course course) {
    	return course.getEditions().stream()
				.map(edition -> new CopyableCourseEdition(edition, copyableAssignmentsFromEdition(edition)))
				.collect(Collectors.toList());

	}

	private static List<CopyableAssignment> copyableAssignmentsFromEdition(CourseEdition edition) {
    	return edition.getAssignments().stream()
				.map(assignment ->  new CopyableAssignment(assignment.getAssignmentId(), assignment.getName(),
						assignment.getSummary()))
				.collect(Collectors.toList());
	}

	@Value
	private static class CopyableCourseEdition {
		private long courseEdition;
		private String name;
		private List<CopyableAssignment> assignments;


		CopyableCourseEdition(CourseEdition edition, List<CopyableAssignment> assignments) {
			this.courseEdition = edition.getId();
			this.name = edition.getName() + " " + edition.intervalString();
			this.assignments = assignments;
		}
	}

	@Value
	private static class CopyableAssignment {
		private long assignmentId;
		private String name;
		private String summary;
	}

    /**
     * Submit a create assignment form
     * @param courseCode the course to create an assignment for
	 * @param editionCode the course to create an assignment for
     * @param name name for the assignment
     * @param summary summary for the assignment
     * @param dueDate due date for the assignment
     * @return a Response containing the generated page
     */
    @POST
    @Path("create")
    public Response createPage(@PathParam("courseCode") String courseCode,
							   @PathParam("editionCode") String editionCode,
                               @FormParam("id") Long assignmentId,
                               @FormParam("courseEditionToCopyFromId") String courseEditionToCopyFromId,
							   @FormParam("assignmentToCopyFromId") String assignmentToCopyFromId,
                               @FormParam("name") String name,
                               @FormParam("summary") String summary,
                               @FormParam("due-date") String dueDate) {

        CourseEdition course = courses.find(courseCode, editionCode);
        if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
            throw new UnauthorizedException();
        }

		if(assignmentsDAO.exists(course, assignmentId)) {
            return redirect(course.getURI().resolve("assignments/create?error=error.assignment-number-exists"));
        }

        Assignment assignment = new Assignment();
        assignment.setCourseEdition(course);
        assignment.setAssignmentId(assignmentId);
        assignment.setName(name);
        assignment.setSummary(summary);

		List<Task> tasks = this.tasksForNewAssignment(assignment, courseEditionToCopyFromId, assignmentToCopyFromId);

        assignment.setTasks(tasks);

        if(!Strings.isNullOrEmpty(dueDate)) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
            try {
                assignment.setDueDate(simpleDateFormat.parse(dueDate));
            }
            catch (ParseException e) {
                return redirect(course.getURI().resolve("assignments/create?error=error.invalid-date-format"));
            }
        }

        try {
            assignmentsDAO.persist(assignment);
        }
        catch (ConstraintViolationException e) {
            Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
            if(violations.isEmpty()) {
                return redirect(course.getURI().resolve("assignments/create?error=error.assignment-create-error"));
            }
            return redirect(course.getURI().resolve("assignments/create?error=" + violations.iterator().next().getMessage()));
        }
        catch (Exception e) {
            return redirect(course.getURI().resolve("assignments/create?error=error.assignment-create-error"));
        }

        return redirect(course.getURI());
    }

    private List<Task> tasksForNewAssignment(Assignment newAssignment, String courseEditionId, String assignmentId) {
    	try {
    		long courseEdition = Long.parseLong(courseEditionId);
    		long assignment = Long.parseLong(assignmentId);

			CourseEdition editionToGetRubricsFrom = courses.find(courseEdition);
			Assignment assignmentToCopyRubricsFrom = assignmentsDAO.find(editionToGetRubricsFrom, assignment);

			return newAssignment.copyTasksFromOldAssignment(assignmentToCopyRubricsFrom);
    	} catch (NumberFormatException e) {
			return Lists.newArrayList();
		}
	}

    /**
     * An overview page for an assignment
     * @param courseCode the course to create an assignment for
	 * @param editionCode the course to create an assignment for
     * @param assignmentId the assignment id
     * @return a Response containing the generated page
     */
    @GET
    @Transactional
    @Path("{assignmentId : \\d+}")
    public Response getAssignmentPage(@PathParam("courseCode") String courseCode,
									  @PathParam("editionCode") String editionCode,
                                      @PathParam("assignmentId") Long assignmentId) throws IOException {

        CourseEdition course = courses.find(courseCode, editionCode);
        if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
            throw new UnauthorizedException();
        }

        Assignment assignment = assignmentsDAO.find(course, assignmentId);

        List<Delivery> currentUserDeliveries = assignedTAs.getLastDeliveries(assignment, currentUser);
        List<Delivery> allLastDeliveries = deliveriesDAO.getLastDeliveries(assignment);
        List<Delivery> filteredDeliveries = deliveriesDAO.getLastDeliveries(assignment);
        filteredDeliveries.removeAll(currentUserDeliveries);

        AssignmentStats userStats = deliveriesBackend.getAssignmentStats(currentUserDeliveries);
        AssignmentStats lastStats = deliveriesBackend.getAssignmentStats(assignment, allLastDeliveries);



        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("user", currentUser);
        parameters.put("course", course);
        parameters.put("assignment", assignment);
        parameters.put("userStats", userStats);
        parameters.put("lastStats", lastStats);
        parameters.put("deliveryStates", Delivery.State.values());
        parameters.put("userDeliveries", currentUserDeliveries);
        parameters.put("lastDeliveries", allLastDeliveries);
        parameters.put("filteredDeliveries", filteredDeliveries);

        List<Locale> locales = Collections.list(request.getLocales());
        return display(templateEngine.process("courses/assignments/assignment-view.ftl", locales, parameters));
    }

    private final static String TEXT_CSV = "text/csv";

    /**
     * Download the grades for this assignment
     * @param courseCode the course to create an assignment for
	 * @param editionCode the course to create an assignment for
     * @param assignmentId the assignment id
     * @return a CSV file with the most recent deliveries
     */
    @GET
    @Transactional
    @Produces(TEXT_CSV)
    @Path("{assignmentId : \\d+}/deliveries/download")
    public String downloadAssignmentResults(@PathParam("courseCode") String courseCode,
											@PathParam("editionCode") String editionCode,
                                            @PathParam("assignmentId") Long assignmentId) throws IOException {

        CourseEdition course = courses.find(courseCode, editionCode);
        Assignment assignment = assignmentsDAO.find(course, assignmentId);

        if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
            throw new UnauthorizedException();
        }

        StringBuilder sb = new StringBuilder();
        CSVPrinter csvPrinter = new CSVPrinter(sb, CSVFormat.RFC4180);
        csvPrinter.printRecord("Assignment", "NetId", "StudentNo", "Name", "Group", "State", "Grade", "Points");

        for (Entry<User, Delivery> entry : deliveriesDAO.getLastDeliveriesByUser(assignment).entrySet()) {
            Delivery delivery = entry.getValue();
            Review review = delivery.getReview();
            User user = entry.getKey();

            csvPrinter.printRecord(
                assignment.getName(),
                user.getNetId(),
                user.getStudentNumber(),
                user.getName(),
                delivery.getGroup().getGroupName(),
                review != null ? review.getState() : State.SUBMITTED,
                review != null ? review.getGrade() : "",
                delivery.getAchievedNumberOfPoints()
            );
        }

		response.addHeader("Content-Disposition", " attachment; filename=\"assignment_" + assignmentId.toString()+ "_grades.csv\"");
        return sb.toString();
    }

	/**
	 * Download the grades for this assignment
	 * @param courseCode the course to create an assignment for
	 * @param editionCode the course to create an assignment for
	 * @param assignmentId the assignment id
	 * @return a CSV file with the most recent deliveries
	 */
	@GET
	@Transactional
	@Produces(TEXT_CSV)
	@Path("{assignmentId : \\d+}/deliveries/download-rubrics")
	public String downloadRubrics(@PathParam("courseCode") String courseCode,
								  @PathParam("editionCode") String editionCode,
								  @PathParam("assignmentId") Long assignmentId) throws IOException {

		CourseEdition course = courses.find(courseCode, editionCode);
		Assignment assignment = assignmentsDAO.find(course, assignmentId);

		if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
			throw new UnauthorizedException();
		}
		StringBuilder sb = new StringBuilder();

		int numLevels = assignment.getTasks().stream()
			.map(Task::getCharacteristics).flatMap(Collection::stream)
			.map(Characteristic::getLevels).mapToInt(Collection::size)
			.max().orElse(0);

		List<Delivery> deliveries = Lists.newArrayList(deliveriesDAO.getLastDeliveries(assignment));
		Collections.sort(deliveries, Delivery.DELIVERIES_BY_GROUP_NUMBER);

        CSVPrinter csvPrinter = new CSVPrinter(sb, CSVFormat.RFC4180);
		// Skip initial columns, list all groups
        for (int i = 0; i < 2 + numLevels; i++) {
            csvPrinter.print("");
        }

        // Print group numbers
        for (Delivery delivery : deliveries) {
            csvPrinter.print("Group " + delivery.getGroup().getGroupNumber());
        }
        csvPrinter.println();

        for (Task task : assignment.getTasks()) {
            // Print exercise
            csvPrinter.print(task.getDescription());
            csvPrinter.print("");
            csvPrinter.println();

            for (Characteristic characteristic : task.getCharacteristics()) {
                csvPrinter.print(characteristic.getDescription());
                csvPrinter.print(characteristic.getWeight());

                for (Mastery mastery : characteristic.getLevels()) {
                    csvPrinter.print(mastery.getDescription());
                }

                for (int i = 0; i < numLevels - characteristic.getLevels().size(); i++) {
                    csvPrinter.print("");
                }

                for (Delivery delivery : deliveries) {
                    if (delivery.getRubrics().containsKey(characteristic)) {
                        csvPrinter.print(
                            delivery.getRubrics().get(characteristic).getPoints()
                        );
                    }
                    else {
                        csvPrinter.print("");
                    }
                }

                csvPrinter.println();
            }
            csvPrinter.println();
        }

		response.addHeader("Content-Disposition", " attachment; filename=\"assignment_" + assignmentId.toString()+ "_rubrics.csv\"");
		return sb.toString();
	}

    /**
     * An edit page page for an assignment
     * @param courseCode the course to create an assignment for
	 * @param editionCode the course to create an assignment for
     * @param assignmentId the assignment id
     */
    @GET
    @Transactional
    @Path("{assignmentId : \\d+}/edit")
    public Response getEditAssignmentPage(@PathParam("courseCode") String courseCode,
										  @PathParam("editionCode") String editionCode,
                                          @PathParam("assignmentId") long assignmentId,
                                          @QueryParam("error") String error) throws IOException {


        CourseEdition course = courses.find(courseCode, editionCode);
        Assignment assignment = assignmentsDAO.find(course, assignmentId);

        if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
            throw new UnauthorizedException();
        }

        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("user", currentUser);
        parameters.put("course", course);
        parameters.put("assignment", assignment);

        if(error != null)
            parameters.put("error", error);

        List<Locale> locales = Collections.list(request.getLocales());
        return display(templateEngine.process("courses/assignments/create-assignment.ftl", locales, parameters));
    }

    @POST
    @Transactional
    @Path("{assignmentId : \\d+}/edit")
    public Response editAssignment(@PathParam("courseCode") String courseCode,
								   @PathParam("editionCode") String editionCode,
                                   @PathParam("assignmentId") long assignmentId,
                                   @FormParam("name") String name,
                                   @FormParam("summary") String summary,
                                   @FormParam("due-date") String dueDate,
                                   @FormParam("release") String release) {

        CourseEdition course = courses.find(courseCode, editionCode);

        if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
            throw new UnauthorizedException();
        }

        Assignment assignment = assignmentsDAO.find(course, assignmentId);
        assignment.setName(name);
        assignment.setSummary(summary);

        //a little bit ugly, form params don't work nicely with checkboxes.
        boolean gradesReleased = CHECKBOX_CHECKED_VALUE.equals(release);
        if (gradesReleased && !assignment.isGradesReleased() && !assignment.getTasks().isEmpty()) {
            releaseGrades(assignment);
        }
        assignment.setGradesReleased(gradesReleased);

        if(!Strings.isNullOrEmpty(dueDate)) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
            try {
                assignment.setDueDate(simpleDateFormat.parse(dueDate));
            }
            catch (ParseException e) {
                return redirect(course.getURI().resolve("assignments/create?error=error.invalid-date-format"));
            }
        }
        else {
            assignment.setDueDate(null);
        }

        try {
            assignmentsDAO.merge(assignment);
        }
        catch (ConstraintViolationException e) {
            Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
            if(violations.isEmpty()) {
                return redirect(course.getURI().resolve("assignments/" + assignmentId + "/edit?error=error.assignment-create-error"));
            }
            return redirect(course.getURI().resolve("assignments/" + assignmentId + "/edit?error=" + violations.iterator().next().getMessage()));
        }
        catch (Exception e) {
            return redirect(course.getURI().resolve("assignments/" + assignmentId + "/edit?error=error.assignment-create-error"));
        }

        return redirect(course.getURI());
    }

    @POST
    @Path("{assignmentId : \\d+}/release-grades")
    public void releaseGrades(@PathParam("courseCode") String courseCode,
                              @PathParam("editionCode") String editionCode,
                              @PathParam("assignmentId") long assignmentId) {

        CourseEdition course = courses.find(courseCode, editionCode);

        if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
            throw new UnauthorizedException();
        }

        Assignment assignment = assignmentsDAO.find(course, assignmentId);
        releaseGrades(assignment);
    }

    /**
     * Release the grades for a course.
     * @param assignment Assignment to release grades for.
     */
    @Transactional
    protected void releaseGrades(Assignment assignment) {
        List<Delivery> deliveries = deliveriesDAO.getLastDeliveries(assignment);
        GradingStrategy gradingStrategy = assignment.getGradingStrategy();

        deliveries.forEach(delivery -> {
            if (delivery.getMasteries().isEmpty()) {
                log.info("Skipping {} as it has no masteries", delivery);
                return;
            }
            Review review = getOrCreateReview(assignment, delivery);
            State previousState = review.getState();

            double grade = gradingStrategy.createGrade(delivery);
            if (review.getGrade() != null) {
                grade = Math.max(grade, review.getGrade());
            }

            review.setGrade(grade);
            review.setState(gradingStrategy.createState(delivery));
            log.info("Updated review {} for {}", review, delivery);

            if (previousState == null || previousState.equals(State.SUBMITTED)) {
                reviewMailer.sendReviewMail(delivery);
            }

            deliveriesDAO.merge(delivery);
        });
    }

    /**
     * Get the {@code Review} from the {@link Assignment}, or try to find a {@code Review}
     * in one of the other {@link Delivery Deliveries}.
     * @param assignment Assignment for which this is a delivery.
     * @param delivery Delivery to find a review for.
     * @return Review instance.
     * @deprecated Only used for SQT where a review may be created on an older submission,
     *  while the latest solution is a resubmission.
     */
    @Deprecated
    private Review getOrCreateReview(Assignment assignment, Delivery delivery) {
        return Optional.ofNullable(delivery.getReview())
            .orElseGet(() -> {
                Review r = deliveriesDAO.getDeliveries(assignment, delivery.getGroup()).stream()
                    .map(Delivery::getReview)
                    .filter(Objects::nonNull)
                    .findAny().orElseGet(() -> {
                        Review review = new Review();
                        review.setReviewUser(currentUser);
                        review.setCommentary("");
                        review.setReviewTime(new Date());
                        return review;
                    });
                delivery.setReview(r);
                return r;
            });
    }

    /**
	 * Display the rubrics page for this {@link Assignment}.
	 * @param courseCode the course to create an assignment for.
	 * @param editionCode the course to create an assignment for.
	 * @param assignmentId the assignment id.
	 * @return The rubrics page.
	 * @throws IOException If an I/O error occurs.
	 */
	@GET
	@Transactional
	@Path("{assignmentId : \\d+}/rubrics")
	public Response getEditRubricsPage(@PathParam("courseCode") String courseCode,
									   @PathParam("editionCode") String editionCode,
									   @PathParam("assignmentId") long assignmentId) throws IOException {


		CourseEdition course = courses.find(courseCode, editionCode);
		Assignment assignment = assignmentsDAO.find(course, assignmentId);

		if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
			throw new UnauthorizedException();
		}

		Map<String, Object> parameters = Maps.newHashMap();
		parameters.put("user", currentUser);
		parameters.put("course", course);
		parameters.put("assignment", assignment);

		List<Locale> locales = Collections.list(request.getLocales());
		return display(templateEngine.process("courses/assignments/assignment-rubrics.ftl", locales, parameters));
	}

	/**
	 * Retrieve the {@link Assignment} as JSON.
	 * @param courseCode the course to create an assignment for.
	 * @param editionCode the course to create an assignment for.
	 * @param assignmentId the assignment id.
	 * @return The {@link Assignment} instance.
	 */
	@GET
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{assignmentId : \\d+}/json")
	public Assignment getAssignmentAsJson(@PathParam("courseCode") String courseCode,
										  @PathParam("editionCode") String editionCode,
										  @PathParam("assignmentId") long assignmentId) {

		CourseEdition course = courses.find(courseCode, editionCode);

		if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
			throw new UnauthorizedException();
		}

		Assignment assignment = assignmentsDAO.find(course, assignmentId);
		// Trigger lazy initialization of tasks...
		assignment.getTasks().size();
		return assignment;
	}

	/**
	 * Update an {@link Assignment}.
	 * @param courseCode the course to create an assignment for.
	 * @param editionCode the course to create an assignment for.
	 * @param assignmentId the assignment id.
	 * @param assignment the updated assignment instance.
	 * @return The updated assignment instance.
	 */
	@PUT
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("{assignmentId : \\d+}/json")
	public Assignment updateAssignment(@PathParam("courseCode") String courseCode,
									   @PathParam("editionCode") String editionCode,
									   @PathParam("assignmentId") long assignmentId,
									   @Valid Assignment assignment) {

		CourseEdition course = courses.find(courseCode, editionCode);

		if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
			throw new UnauthorizedException();
		}

		assignment.setCourseEdition(course);
		assignment.setAssignmentId(assignmentId);
		return assignmentsDAO.merge(assignment);
	}

	/**
	 * Get the last assignment deliveries as JSON.
	 * @param courseCode the course to create an assignment for.
	 * @param editionCode the course to create an assignment for.
	 * @param assignmentId the assignment id.
	 * @return a list of deliveries.
	 */
	@GET
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{assignmentId : \\d+}/last-deliveries/json")
	public List<Delivery> getLastDeliveries(@PathParam("courseCode") String courseCode,
											@PathParam("editionCode") String editionCode,
											@PathParam("assignmentId") long assignmentId) {
		CourseEdition course = courses.find(courseCode, editionCode);
		Assignment assignment = assignmentsDAO.find(course, assignmentId);

		if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
			throw new UnauthorizedException();
		}

		List<Delivery> deliveries = deliveriesDAO.getLastDeliveries(assignment);
		// Lazy load...
		deliveries.forEach(Delivery::getMasteries);
		return deliveries;
	}

	@POST
    @Transactional
    @Path("{assignmentId : \\d+}/distribute-tas")
    public Response distributeTAs(@PathParam("courseCode") String courseCode,
                                  @PathParam("editionCode") String editionCode,
                                  @PathParam("assignmentId") long assignmentId) {
        CourseEdition course = courses.find(courseCode, editionCode);
        Assignment assignment = assignmentsDAO.find(course, assignmentId);

        if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
            throw new UnauthorizedException();
        }

        List<Delivery> deliveries = deliveriesDAO.getLastDeliveries(assignment);
        List<Group> groups = Lists.transform(deliveries, Delivery::getGroup);
        Set<User> TAs = course.getAssistants();

        groups.removeAll(Lists.transform(assignment.getAssignedTAS(), AssignedTA::getGroup));

        List<AssignedTA> assignedTAS = assignGroups(TAs, groups, assignment, ThreadLocalRandom.current());

        assignedTAS.forEach(this.assignedTAs::persist);

        return Response.seeOther(assignment.getURI()).build();
    }
}
