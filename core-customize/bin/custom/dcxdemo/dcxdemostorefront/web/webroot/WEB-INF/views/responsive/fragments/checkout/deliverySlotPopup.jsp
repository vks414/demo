<%@ page trimDirectiveWhitespaces="true"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="cms" uri="http://hybris.com/tld/cmstags"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="format" tagdir="/WEB-INF/tags/shared/format"%>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="format" tagdir="/WEB-INF/tags/shared/format"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<spring:htmlEscape defaultHtmlEscape="true" />

         <div id="deliverySlot" class="deliverySlot">
            <span class="pull-left pb-3"><spring:theme code="checkout.select.slot"  arguments="${daysToShow}"/></span>
            <div class="col-xs-12 d-flex">
             <spring:url var="selectDeliverySlotUrl" value="{contextPath}/checkout/multi/delivery-address/selectslot" htmlEscape="false">
                                             <spring:param name="contextPath" value="${request.contextPath}" />
                                         </spring:url>
            <c:forEach items="${deliverySlotDateList}" var="deliverySlotDate" varStatus="status">
                <div class="col deliverySlot_content pl-0">
                        <ul class="p-0 mb-0">
                            <li>
                                <strong><fmt:formatDate type="date" value="${deliverySlotDate}"
							dateStyle="FULL" />&nbsp;</strong>
                                <br /><br />
                               <c:forEach items="${deliverySlotDataList}" var="deliverySlotTimings" varStatus="status"> 
                              <c:if test="${deliverySlotTimings.date eq deliverySlotDate}">
                              		<form class="deliverySlotForm" action="${fn:escapeXml(selectDeliverySlotUrl)}" method="get">
                               				<input type="hidden" name="delivery_slot" value="${deliverySlotTimings.PK}" />
                               				<c:forEach items="${fn:split(entries,',')}" var="entryNumber">
                               				<c:if test="${not empty entryNumber}">
         									<input type="hidden" value="${entryNumber}" name="entries"> 
                               				</c:if>
                               				</c:forEach>
			                               <c:choose>
                               					<c:when test="${deliverySlotTimings.remainingCapacity ne 0}">
					                               	<div id="js_deliveryslot_index_${status.index}" class="deliveryslot_list js_deliveryslot_submit <c:if test="${deliverySlotTimings.date eq selectedSlotDateFmt && deliverySlotTimings.fromTime eq fromTime &&  deliverySlotTimings.toTime eq toTime}">active</c:if><c:if test="${selectedSlotDateFmt eq null && deliverySlotTimings.date eq EarliestSlotDetail.date && deliverySlotTimings.fromTime eq EarliestSlotFromTime &&  deliverySlotTimings.toTime eq EarliestSlotToTime}">active</c:if>">
					                               		${fn:escapeXml(deliverySlotTimings.fromTime)} &nbsp;-&nbsp; ${fn:escapeXml(deliverySlotTimings.toTime)}
		                               						<span class="pull-right"><spring:theme code="checkout.slot.available"/></span>
					                               <div class="clearfix"></div>	
					                               </div>
                               					</c:when>
                               					<c:otherwise>
													<div class="deliveryslot_list unavailable_slot">
					                               		${fn:escapeXml(deliverySlotTimings.fromTime)} &nbsp;-&nbsp; ${fn:escapeXml(deliverySlotTimings.toTime)}
		                               						<span class="pull-right"><spring:theme code="checkout.slot.unavailable"/></span>
					                               <div class="clearfix"></div>			                               						
					                               </div>
                               					</c:otherwise>
                               				</c:choose>
                                	 </form>
                                	  </c:if>
                              </c:forEach> 
                            </li>
                        </ul>
                </div>
            </c:forEach>
            </div>
        </div><br>
<br>
