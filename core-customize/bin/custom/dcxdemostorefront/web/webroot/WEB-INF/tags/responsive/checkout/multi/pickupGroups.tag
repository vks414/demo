<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="cartData" required="true" type="de.hybris.platform.commercefacades.order.data.CartData" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="theme" tagdir="/WEB-INF/tags/shared/theme" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<spring:htmlEscape defaultHtmlEscape="true" />

<c:forEach items="${cartData.pickupOrderGroups}" var="groupData" varStatus="status">
    <hr/>
	<div class="checkout-shipping-items row">
        <div class="col-sm-12 col-lg-6 checkout-pickup-items">
            <div class="checkout-shipping-items-header">
                <spring:theme code="checkout.multi.pickup.items" arguments="${status.index + 1},${fn:length(groupData.entries)}"
                              text="Pick Up # ${status.index + 1} - ${fn:length(groupData.entries)} Item(s)">
                </spring:theme>
            </div>

            <ul>
                <c:set var="groupentries" value=""/>
                <c:forEach items="${groupData.entries}" var="entry">
                <c:set var="groupentries" value="${groupentries},${entry.entryNumber}"/>
                    <li class="row">
                        <span class="name col-xs-8">${fn:escapeXml(entry.product.name)}</span>
                        <span class="qty col-xs-4"><spring:theme code="basket.page.qty"/>&nbsp;${fn:escapeXml(entry.quantity)}</span>
                    </li>
                </c:forEach>
            </ul>
        </div>

        <div class="col-sm-12 col-lg-6">
            <div class="checkout-shipping-items-header">
                <spring:theme code="checkout.multi.inStore"/>
            </div>

			<strong>${fn:escapeXml(groupData.deliveryPointOfService.name)}</strong>
			<br>
			<c:if test="${ not empty groupData.deliveryPointOfService.address.line1 }">
				${fn:escapeXml(groupData.deliveryPointOfService.address.line1)},&nbsp;
			</c:if>
			<c:if test="${ not empty groupData.deliveryPointOfService.address.line2 }">
				${fn:escapeXml(groupData.deliveryPointOfService.address.line2)},&nbsp;
			</c:if>
			<c:if test="${not empty groupData.deliveryPointOfService.address.town }">
				${fn:escapeXml(groupData.deliveryPointOfService.address.town)},&nbsp;
			</c:if>
			<c:if test="${ not empty groupData.deliveryPointOfService.address.region.name }">
				${fn:escapeXml(groupData.deliveryPointOfService.address.region.name)},&nbsp;
			</c:if>
			<c:if test="${ not empty groupData.deliveryPointOfService.address.postalCode }">
				${fn:escapeXml(groupData.deliveryPointOfService.address.postalCode)},&nbsp;
			</c:if>
			<c:if test="${ not empty groupData.deliveryPointOfService.address.country.name }">
				${fn:escapeXml(groupData.deliveryPointOfService.address.country.name)}
			</c:if>
            <br/>
			<c:if test="${ not empty groupData.deliveryPointOfService.address.phone }">
				${fn:escapeXml(groupData.deliveryPointOfService.address.phone)}
			</c:if>
			<c:forEach items="${groupData.entries}" var="entry" end="0">
				<c:if test="${not empty entry.deliverySlot}">
				<br>
					<b>Earliest/Selected Slot:&nbsp;</b> <fmt:formatDate type="date" value="${entry.deliverySlot.date}" dateStyle="FULL" />&nbsp;(${entry.deliverySlot.fromTime} - ${entry.deliverySlot.toTime}) 
				</c:if>	
			</c:forEach>
			<br>
			<br>
		<a href="getslots?posName=${fn:escapeXml(groupData.deliveryPointOfService.name)}&entries=${groupentries}" class=" edit_timeslot btn btn-primary">Edit Slot</a>
		
		</div>
	</div>
		<style>#cboxTitle {
	padding: 16px;
	height: auto;
	background: #F2F0ED;
}

#cboxTitle .headline {
	margin-bottom: 0px;
	font-size: 14px;
}

#cboxLoadedContent {
    margin-top: 56px;
    padding: 16px;
	max-height: calc(100vh - 130px);
}

#cboxContent {
	max-height: calc(100vh - 40px);
}
.col.deliverySlot_content.pl-0 {
    min-width: 195px;
}
</style>
</c:forEach>
