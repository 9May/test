<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
<head>
	<title>ERP Demo - Creating New Sales</title>
</head>
<body>
<h2><spring:message code="module.sales.create.title"/></h2>
<form:form method="post" action="save.html" commandName="sales">
	<table>
	<tr>
		<td><form:label path="salesName"><spring:message code="entity.sales.name"/></form:label></td>
		<td><form:input path="salesName" /></td>
	</tr>
	<tr>
		<td><form:label path="salesEmail"><spring:message code="entity.sales.email"/></form:label></td>
		<td><form:input path="salesEmail" /></td>
	</tr>
	<tr>
		<td><form:label path="salesPhone"><spring:message code="entity.sales.phone"/></form:label></td>
 		<td><form:input path="salesPhone" /></td>
	</tr>
	<tr>
		<td colspan="2">
			<input type="submit" value="<spring:message code="module.sales.button.add"/>"/>
		</td>
	</tr>
	</table>
</form:form>
</body>
</html>