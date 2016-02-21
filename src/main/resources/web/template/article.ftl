<#setting number_format="0"/>
<html>
	<head>
		<#include "inc_head.ftl">
		<title>Article: ${title!}</title>

		<style>
		.label{color:blue; margin-left: 5px;}
		.link {padding:5px 10px 5px 10px;border: 1px solid royalblue;
			background-color: royalblue; color:white;}
		</style>
	</head>
	<body>
	<#include "header.html">
	<div>
		id: ${id} <br/>
		name: ${title!} <br/>
		alias: <#list aliasNames as name>${name} </#list><br/>
		links: <#list links as link>
		<span class="label">
			<a href="/wiki/article?name=${link}">${link}</a>
		</span>
		</#list>
		<br/><br/>
	</div>

	<div style="maring:10px;">
        <a class="link" href="/wiki/link/${id}">View Inlinks &amp;
	        Outlinks</a>
		<br/><br/>
	</div>

	<div>
		以下是正文：<br/>
		<hr/>
		${content?replace("\n", "<br>")}
	</div>
</body>
</html>