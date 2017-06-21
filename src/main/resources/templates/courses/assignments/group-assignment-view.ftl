[#import "../../macros.ftl" as macros]
[#import "../../components/project-frameset.ftl" as projectFrameset]
[#import "../../components/delivery.ftl" as deliveryElement]

[@macros.renderHeader i18n.translate("section.projects") ]
<link href="//cdnjs.cloudflare.com/ajax/libs/x-editable/1.5.0/bootstrap3-editable/css/bootstrap-editable.css" rel="stylesheet"/>
[/@macros.renderHeader]
[@macros.renderMenu i18n user /]
<div class="container">

    [@projectFrameset.renderBreadcrumb i18n group repositoryEntity/]

    <div class="row">
        <div class="col-md-10 col-md-offset-2">
        [#if user.isAdmin() || user.isAssisting(course)]
            <a href="${assignment.getURI()}" class="btn btn-default pull-right">
                <span class="glyphicon glyphicon-chevron-left"></span>
            ${i18n.translate("assignment.go-back-to-assignment")}
            </a>
        [/#if]
            <h4 style="line-height:34px; margin-top:0;">${assignment.getName()}</h4>
        </div>
    </div>

    <div class="row">
        <div class="col-md-2">
        [@projectFrameset.renderSidemenu "assignments" i18n group repository/]
        </div>
        <div class="col-md-10">
            [#if assignment.getSummary()??]
                [@MarkDownParser message=assignment.getSummary()][/@MarkDownParser]
            [/#if]

            [#if assignment.getDueDate()??]
                <p>
                    <strong>Due date</strong>
                    <span>${assignment.getDueDate()}</span>
                </p>
            [/#if]

            <table class="table table-bordered">
            [#if myDeliveries?? && myDeliveries?has_content]
                [#list myDeliveries as delivery]
                <tr>
                    <td>
                        [@deliveryElement.render delivery builds/]
                    [#if user.isAdmin() || user.isAssisting(course)]
                        [#if delivery_index == 0]
                        <div class="pull-right">
                          <a href="deliveries/${delivery.getDeliveryId()?c}/review" class="btn btn-default">${i18n.translate("button.label.review")}</a>
                        </div>
                        [#else]
	                      <a href="deliveries/${delivery.getDeliveryId()?c}/review" class="btn btn-link">${i18n.translate("button.label.previous-review")}</a>
                        [/#if]
                    [/#if]
                    </td>
                </tr>
                [/#list]
            [#else]
                <tr>
                    <td>
                [#if user.isAdmin() || user.isAssisting(course)]
                    ${i18n.translate("assignment.no-submission.assistant")}
                [#else]
                    ${i18n.translate("assignment.no-submission.member")}
                [/#if]
                    </td>
                </tr>
            [/#if]
            </table>

            [#if canSubmit?? && canSubmit && group.getMembers()?seq_contains(user)]
            <div class="panel panel-default">
                <div class="panel-heading">${i18n.translate("assignment.submit.title")}</div>
                <div class="panel-body">
                    <form action="" method="post" target="_self" enctype="multipart/form-data">
                        <div class="form-group">
                            <label>${i18n.translate("course.control.assignment")}</label>
                            <input type="text" class="form-control" value="${assignment.getName()}" disabled>
                        </div>

                        <div class="form-group">
                            <label for="commit-id">${i18n.translate("assignment.commit")}</label>
                            <select class="form-control" name="commit-id" id="commit-id">
                    [#if recentCommits?? && recentCommits?has_content]
                        [#list recentCommits as commit]
                                <option value="${commit.getCommit()}">${commit.getMessage()} (${(commit.getTime() * 1000)?number_to_datetime?string["EEEE dd MMMM yyyy HH:mm"]})</option>
                        [/#list]
                    [#else]
                                <option value="">${i18n.translate("assignment.no-commit")}</option>
                    [/#if]
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="notes">${i18n.translate("delivery.notes")}</label>
                            <textarea class="form-control" name="notes" id="notes" rows="3"></textarea>
                        </div>

                        <div class="form-group">
                            <label for="file-attachment">${i18n.translate("delivery.file-attachment")}</label>
                            <input type="file" id="file-attachment" name="file-attachment">
                        </div>

                        <button type="submit" class="btn btn-primary pull-right">${i18n.translate("button.label.submit")}</button>
                    </form>
                </div>
            </div>
            [/#if]
        </div>
    </div>

</div>
[@macros.renderScripts ]
<script src="//cdnjs.cloudflare.com/ajax/libs/x-editable/1.5.0/bootstrap3-editable/js/bootstrap-editable.min.js"></script>
<script type="text/javascript">
	$('#group').editable({
		showbuttons: false
	});
</script>
[/@macros.renderScripts]
[@macros.renderFooter /]
