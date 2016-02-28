<#setting number_format="0"/>
<html>
	<head>
		<#include "inc_head.ftl">
		<title>Explicit Semantic Analysis(ESA & ESPM)</title>
	</head>
	<body>
	<#include "header.html">
	<div>
		<form action="esa" method="post">
			<textarea class="form-control" name="t"
			          style="width:600px;height:100px;">${t!"微博"}</textarea>
			<br/>
			<button type="submit">Analyze</button>
        </form>
	</div>
	<div>
		Explicit Semantic Paths:
			<ol>
				<#if paths?exists>
				    <#list paths as path>
				        <li>${path.pathString}</li>
				    </#list>
				</#if>
			</ol>
		    <ol>
        Explicit Concepts:
		    <#if concepts?exists>
		    <#list concepts as c>
                <li>
	                <a href="/wiki/article/${c.outId}">${c.outId}</a>
                    ${c.name} ${c.value?string["0.###"]}
                </li>
		    </#list>
		    </ol>
			</#if>
	</div>
</body>
</html>