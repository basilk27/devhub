[#import "../../macros.ftl" as macros]
[#import "../../components/commit-row.ftl" as commitRow]

[@macros.renderHeader i18n.translate("assignments.title") /]
[@macros.renderMenu i18n user /]

[#macro deliveryTable deliveries tableid]
<table class="table table-bordered" id="${tableid}">
    [#if deliveries?? && deliveries?has_content]
        [#list deliveries as delivery]
            [#assign group = delivery.getGroup()]
            <tr class="delivery ${delivery.getState().toString()?lower_case}">
                <td class="commit">
                    <a href="${group.getURI()}assignments/${assignment.getAssignmentId()}">
                        <div class="pull-right">
                          [#if delivery.isLate()]
                              <span class="label label-danger">${i18n.translate("assignment.handed-in-late")}</span>
                          [/#if]

                          [#assign review = delivery.getReview()![]]
                          [#if review?has_content && review.grade?? && review.grade?has_content]
                              <span class="label label-default">${review.grade?string["0.#"]}</span>
                          [/#if]

                          [#assign state = delivery.getState()]
                          <span class="label label-${state.style}" data-toggle="tooltip"
                                title="${i18n.translate(state.descriptionTranslionKey)}">
                          ${i18n.translate(state.translationKey)}
                          </span>

                          [#assign assignedTA = assignment.getAssignedTA(delivery).orElse(null)![]]
                          [#if assignedTA?? && assignedTA?has_content]
                              <span class="label label-default">${assignedTA.getName()}</span>
                          [/#if]

                          [#if delivery.assignment.characteristics?size > 0]
                            <div class="progress progress-grading" title="${delivery.rubrics?keys?size / delivery.assignment.characteristics?size * 100}% complete">
                              <div class="progress-bar [#if delivery.rubrics?keys?size == delivery.assignment.characteristics?size ]progress-bar-success[/#if]" role="progressbar" aria-valuenow="${delivery.rubrics?keys?size}"
                                   aria-valuemin="0" aria-valuemax="${delivery.assignment.characteristics?size}"
                                   style="width:${delivery.rubrics?keys?size / delivery.assignment.characteristics?size * 100}%">
                              </div>
                              <span>${(delivery.rubrics?keys?size / delivery.assignment.characteristics?size * 100)?round}%</span>
                            </div>
                          [/#if]
                        </div>
                        <div class="comment"><strong>${delivery.getGroup().getGroupName()}</strong></div>
                        <div class="committer">${delivery.createdUser.getName()}
                            on ${delivery.getTimestamp()?string["EEEE dd MMMM yyyy HH:mm"]}</div>
                    </a>
                </td>
            </tr>
        [/#list]
    [#else]
        <tr>
            <td class="muted">${i18n.translate("assignment.no-deliveries")}</td>
        </tr>
    [/#if]
</table>
[/#macro]

[#macro progressBar assignmentStats barId]
<div class="well well-sm" id="${barId}">
    [#if barId == "user"]
        <h5><strong>Your Progress</strong></h5>
    [#elseif barId == "last"]
        <h5><strong>Total Progress</strong></h5>
    [#else ]
        <h5><strong>Progress</strong></h5>
    [/#if]


    <div class="progress">
        [#list deliveryStates as state]
            <div class="progress-bar progress-bar-${state.style}"
                 style="width: ${assignmentStats.getPercentageFor(state)}%">
            ${i18n.translate(state.translationKey)}
            </div>
        [/#list]
    </div>

    <div class="row">
        [#list deliveryStates as state]
            <div class="col-md-2 progress-info">
                <span class="text-${state.style} glyphicon glyphicon-stop"></span>
                <button class="btn btn-link delivery-filter" data-filter-class="${state.toString()?lower_case}"
                        data-toggle="tooltip" title="${i18n.translate(state.descriptionTranslionKey)}">
                    <span>${i18n.translate(state.translationKey)}:</span>
                    <span>${assignmentStats.getCountFor(state)}</span>
                    <span>(${assignmentStats.getPercentageFor(state)}%)</span>
                </button>
            </div>
        [/#list]
        <div class="col-md-2 progress-info">${i18n.translate("assignment.submissions")}
            : ${assignmentStats.amountOfSubmissions()}</div>
        <div class="col-md-2 progress-info">${i18n.translate("assignment.groups")}
            : ${assignmentStats.amountOfGroups()}</div>
    </div>
</div>
[/#macro]

<div class="container">

    <ol class="breadcrumb">
        <li><a href="/courses">${ i18n.translate("section.courses") }</a></li>
        <li><a href="${course.course.getURI()}">${course.course.code} - ${course.course.name}</a></li>
        <li>
          <span uib-dropdown dropdown-append-to-body="true">
            <a href id="simple-dropdown" uib-dropdown-toggle>
              ${course.timeSpan.start?string["yyyy"]}[#if course.timeSpan.end??]
                  - ${course.timeSpan.end?string["yyyy"]}[/#if]
                  <span class="caret"></span>
            </a>
            <ul uib-dropdown-menu>
            [#list course.course.getEditions() as a]
                <li><a href="${a.getURI()}">${a.timeSpan.start?string["yyyy"]}[#if a.timeSpan.end??]
                    - ${a.timeSpan.end?string["yyyy"]}[/#if]</a></li>
            [/#list]
            </ul>
          </span>
        </li>
        <li><a href="${course.getURI()}">${ i18n.translate("assignments.title") }</a></li>
        <li>
          <span uib-dropdown dropdown-append-to-body="true">
            <a href id="simple-dropdown" uib-dropdown-toggle>
              ${assignment.getName()}
                  <span class="caret"></span>
            </a>
            <ul uib-dropdown-menu>
            [#list course.getAssignments() as a]
                <li><a href="${a.getURI()}">${a.getName()}</a></li>
            [/#list]
            </ul>
          </span>
        </li>
        <li>
          <span uib-dropdown dropdown-append-to-body="true">
            <a href id="simple-dropdown" uib-dropdown-toggle>
              Overview
              <span class="caret"></span>
            </a>
            <ul uib-dropdown-menu>
              <li><a href="${assignment.getURI()}rubrics">Rubrics</a></li>
            </ul>
          </span>
        </li>
    </ol>

[@progressBar assignmentStats=userStats barId="user" /]
[@progressBar assignmentStats=lastStats barId="last" /]

[@deliveryTable deliveries=userDeliveries tableid="user"/]
[@deliveryTable deliveries=lastDeliveries tableid="last"/]
[@deliveryTable deliveries=filteredDeliveries tableid="filtered"/]

    <div class="pull-right">
        <button class="btn btn-default btn-sm toggle-view"><i
                class="glyphicon glyphicon-eye-open"></i>
            Toggle View
        </button>
        <form action="${assignment.getURI()}distribute-tas" method="post" style="display: inline;">
            <button type="submit" class="btn btn-sm btn-default" id="distribute-tas"><i
                    class="glyphicon glyphicon-user"></i> Distribute TAs
            </button>
        </form>
        <a href="${assignment.getURI()}deliveries/download-rubrics" class="btn btn-sm btn-default"
           style="margin-right:5px;"><i class="glyphicon glyphicon-floppy-save"></i> Download rubrics</a>
        <a href="${assignment.getURI()}deliveries/download" class="btn btn-sm btn-default" style="margin-right:5px;"><i
                class="glyphicon glyphicon-floppy-save"></i> Download grades</a>
    </div>

</div>
[@macros.renderScripts]
<script src="/static/js/deliveries-filter.js"></script>
<script src="/static/js/toggle-view.js"></script>
<script src="/static/vendor/angular/angular.min.js"></script>
<script src="/static/vendor/angular-bootstrap/ui-bootstrap.min.js"></script>

<script type="text/javascript">
    angular.module('devhub', ['ui.bootstrap']);
</script>
[/@macros.renderScripts]
[@macros.renderFooter /]
