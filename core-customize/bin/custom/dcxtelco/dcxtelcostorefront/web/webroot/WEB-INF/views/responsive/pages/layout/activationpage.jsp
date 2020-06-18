<%@ page trimDirectiveWhitespaces="true"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="cms" uri="http://hybris.com/tld/cmstags"%>
<%@ taglib prefix="ycommerce" uri="http://hybris.com/tld/ycommercetags" %>
<%@ taglib prefix="template" tagdir="/WEB-INF/tags/responsive/template"%>
<%@ taglib prefix="multi-checkout" tagdir="/WEB-INF/tags/responsive/checkout/multi"%>
<%@ taglib prefix="multi-checkout-telco" tagdir="/WEB-INF/tags/addons/b2ctelcoaddon/responsive/checkout/multi"%>
<%@ taglib prefix="telco-structure" tagdir="/WEB-INF/tags/addons/b2ctelcoaddon/responsive/structure"%>

<spring:htmlEscape defaultHtmlEscape="true" />
 
<div class="row activation-comp">
<div class="col-sm-3">
</div>
<div class="col-sm-6">
            <div class="place-order-form hidden-xs">
                        <label> 
                            To activate you device please enter the device ID here.
                        </label>
            </div>
</div>
    
</div>
 <div class="row">
<div class="col-sm-3">
</div>
<div class="col-sm-6">
                        <input type="text" name="deviceid" id="deviceid">
                       <input type="button" value="Activate" id="activatebutton"/>
                       </div>
</div>
