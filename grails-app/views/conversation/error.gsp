<%--
/*
    This file is part of the grails session-per-conversation plugin.

    session-per-conversation is free software: you can redistribute it and/or modify
    it under the terms of the Lesser GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    session-per-conversation is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    Lesser GNU General Public License for more details.

    You should have received a copy of the Lesser GNU General Public License
    along with AppStatus.  If not, see <http://www.gnu.org/licenses/>.
*/
--%>
<g:if test="${request.xhr}">${exception.message}</g:if>
<g:else>
<html>
  <head>
    <meta name="layout" content="main" />
    <title>Conversational Error</title>
  </head>
  <body id="conversationalErrorPage">
    <div id="message">${exception.message}</div>
  </body>
</html>
</g:else>