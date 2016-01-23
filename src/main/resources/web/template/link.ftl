<#setting number_format="0"/>
<html>
	<head>
		<meta charset="utf-8">
		<title>Inlinks and Outlinks</title>
	</head>
	<body>
	<p>
		id: ${id} <br/>
		name: ${name} <br/>
		alias:<#list alias as a>${a} </#list>
	</p>

	<h2>Inlinks</h2>
	<ol>
	<#list inlinks as link><br>
	<li><span><a href="${link.right}">${link.right}</a></span> ${link.left}</li>
	</#list>
	</ol>

	<h2>Outlinks</h2>
    <ol>
	<#list outlinks as link><br>
    <li><span><a href="${link.right}">${link.right}</a></span> ${link.left}</li>
	</#list>
    </ol>
</body>
</html>