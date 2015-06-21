<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>Error Handling Example</title>
  <meta charset="utf-8">
</head>
<body>

<h2>
  Status code <code><%= request.getAttribute("httpStatusCode") %></code>
</h2>

<p>
  <span>Message: </span>
  <code>
    <pre>
<%= request.getAttribute("message") %>
    </pre>
  </code>
</p>

<p>
  <span>Stack-Trace: </span>
  <code>
    <pre>
<%= request.getAttribute("stackTrace") %>
    </pre>
  </code>
</p>
</body>
</html>