<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Template • TodoMVC</title>
  <link rel="stylesheet" href="static/todomvc-common/base.css">
  <link rel="stylesheet" href="static/todomvc-app-css/index.css">
  <!-- CSS overrides - remove if you don't need it -->
  <link rel="stylesheet" href="static/css/app.css">
</head>
<body>
<section class="todoapp">
  <header class="header">
    <h1>todos</h1>
    <input class="new-todo" placeholder="What needs to be done?" autofocus>
  </header>
  <!-- This section should be hidden by default and shown when there are todos -->
  <section class="main">
    <input class="toggle-all" type="checkbox">
    <label for="toggle-all">Mark all as complete</label>
    <ul class="todo-list">
      <!-- These are here just to show the structure of the list items -->
      <!-- List items should get the class `editing` when editing and `completed` when marked as completed -->
      <li class="completed">
        <div class="view">
          <input class="toggle" type="checkbox" checked>
          <label>Taste JavaScript</label>
          <button class="destroy"></button>
        </div>
        <input class="edit" value="Create a TodoMVC template">
      </li>
      <li>
        <div class="view">
          <input class="toggle" type="checkbox">
          <label>Buy a unicorn</label>
          <button class="destroy"></button>
        </div>
        <input class="edit" value="Rule the web">
      </li>
    </ul>
  </section>
  <!-- This footer should hidden by default and shown when there are todos -->
  <footer class="footer">
    <!-- This should be `0 items left` by default -->
    <span class="todo-count"><strong>0</strong> item left</span>
    <!-- Remove this if you don't implement routing -->
    <ul class="filters ${filter}">
      <li>
        <a href="/todomvc/all">All</a>
      </li>
      <li>
        <a href="/todomvc/active">Active</a>
      </li>
      <li>
        <a href="/todomvc/completed">Completed</a>
      </li>
    </ul>
    <!-- Hidden if no completed items are left ↓ -->
    <button class="clear-completed">Clear completed</button>
  </footer>
</section>
<footer class="info">
  <p>Double-click to edit a todo</p>
  <!-- Remove the below line ↓ -->
  <p>Template by <a href="http://sindresorhus.com">Sindre Sorhus</a></p>
  <!-- Change this out with your name and url ↓ -->
  <p>Created by <a href="http://todomvc.com">you</a></p>
  <p>Part of <a href="http://todomvc.com">TodoMVC</a></p>
</footer>
<!-- Scripts here. Don't remove ↓ -->
<script src="static/todomvc-common/base.js"></script>
<script src="static/js/app.js"></script>
</body>
</html>

