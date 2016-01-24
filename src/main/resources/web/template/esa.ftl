<#setting number_format="0"/>
<html>
	<head>
		<#include "inc_head.ftl">
		<title>Explicit Semantic Analysis(ESA)</title>
	</head>
	<body>
	<#include "header.html">
	<div>
		<form action="esa" method="post">
			<textarea class="form-control" name="t"
			          style="width:600px;height:100px;">${t!}</textarea>
			<br/>
			<input class="btn btn-large" name="DO ESA Analysis" type="submit"/>
        </form>
	</div>
	<div>
		    <ol>
		    <#if concepts?exists>
		    <#list concepts as c>
                <li>
	                <a href="/wiki/article/${c.outId}">${c.outId}</a>
                    ${c.name} ${c.value}
                </li>
		    </#list>
		    </ol>
			</#if>
	</div>
</body>
</html>