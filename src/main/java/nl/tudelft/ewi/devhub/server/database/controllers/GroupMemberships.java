package nl.tudelft.ewi.devhub.server.database.controllers;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.inject.persist.Transactional;
import nl.tudelft.ewi.devhub.server.database.entities.Group;
import nl.tudelft.ewi.devhub.server.database.entities.GroupMembership;
import nl.tudelft.ewi.devhub.server.database.entities.QGroupMembership;
import nl.tudelft.ewi.devhub.server.database.entities.User;

public class GroupMemberships extends Controller<GroupMembership> {

	@Inject
	public GroupMemberships(EntityManager entityManager) {
		super(entityManager);
	}

	@Transactional
	public List<Group> listParticipating(User user) {
		Preconditions.checkNotNull(user);
		return query().from(QGroupMembership.groupMembership)
			.where(QGroupMembership.groupMembership.user.id.eq(user.getId()))
			.where(QGroupMembership.groupMembership.group.course.start.isNotNull())
			.where(QGroupMembership.groupMembership.group.course.end.isNull())
			.orderBy(QGroupMembership.groupMembership.group.course.code.toLowerCase().asc())
			.list(QGroupMembership.groupMembership.group);
	}

	@Transactional
	public List<GroupMembership> ofGroup(Group group) {
		Preconditions.checkNotNull(group);
		return query().from(QGroupMembership.groupMembership)
			.where(QGroupMembership.groupMembership.group.groupId.eq(group.getGroupId()))
			.list(QGroupMembership.groupMembership);
	}

}
