<#setting number_format="0"/>
<html>
	<head>
        <meta charset="utf-8">
		<title>${title!}</title>
		<style>
		.label{color:blue; margin-left: 5px;}
		.link {padding:5px 10px 5px 10px;border: 1px solid royalblue;
			background-color: royalblue; color:white;}
		</style>
	</head>
	<body>
	<div>
		id: ${id} <br/>
		name: ${title!} <br/>
		links: <#list links as link><span class="label">${link}</span> </#list>
		<br/><br/>
	</div>

	<div style="maring:10px;">
        <a class="link" href="../link/${id}">Inlinks &amp; Outlinks</a>
		<br/><br/>
	</div>

	<div>
		以下是正文：<br/>
		<hr/>
		${content?replace("\n", "<br>")}
	</div>
</body>
</html>