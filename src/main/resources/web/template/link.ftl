<#setting number_format="0"/>
<html>
	<head>
		<#include "inc_head.ftl">
		<title>Links - ${name}</title>
	</head>
	<body>
	<#include "header.html">
	<p>
		id: ${id} <br/>
		name: <a href="/wiki/article/${id}">${name}</a> <br/>
		alias:<#list alias as a>${a} </#list>
	</p>

	<h2>Inlinks</h2>
	<ol>
	<#list inlinks as link><br>
	<li>
		<a href="${link.right}">${link.right}</a>
		${link.left}
        <a href="${link.right}">Links</a>
        <a href="/wiki/article/${link.right}">Detail</a>
	</li>
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