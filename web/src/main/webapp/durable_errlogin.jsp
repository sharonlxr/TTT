<%@ include file ="header.jsp" %>

<h1>Authentication Error </h1>


<table width="50%" border="1" align="center" cellpadding="2" cellspacing="1" bgcolor="#F5F5F5" class="tblBg">
  <tr align="center">
    <td class="tblTitle2">You do not have permission to view this page. Your user role does not have enough permisison or you just typed in wrong user name and password. </td>
  </tr>
  <tr>
    <td align="center">
        <input type="button" name="button" value="OK" class="button" onClick="javascript:history.back()">
    </td>
  </tr>
</table>

<%@ include file ="footer.html" %>