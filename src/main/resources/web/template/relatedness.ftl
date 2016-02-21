<#setting number_format="0.######"/>
<html>
	<head>
		<#include "inc_head.ftl">
		<title>Calculate Relatedness</title>
		<style>
			label{width:200px;display: inline-block;}
			li{line-height: 2em;}
		</style>
	</head>
	<body>
	<#include "header.html">
	<div>
		<form action="/relatedness" method="get">
			<ul>
				<li>
					<label for="name1">First Wiki Concept:</label>
					<input name="name1" value="${name1!'法官'}"/>
                </li>
				<li>
			        <label for="name2">Second Wiki Concept:</label>
			        <input name="name2" value="${name2!'法律'}"/>
                </li>
                <li>
					<button type="submit">Calculate Relatedness</button>
                </li>
            </ul>
        </form>
	</div>
	<div>
		    <#if msg?exists>
		        <p style="color:red">${msg}</p>
		    <#else>
		        <h2>
			        Relatedness between
			        <a href="/wiki/link/${id1}">${name1}</a>
			        and <a href="/wiki/link/${id2}">${name2}</a>
		        </h2>
		        <div>
			        The relatedness is
		            <span style="font-size:larger;color: red;">${relatedness}</span>
		        </div>

		        <h2>Inlink intersection</h2>
		        <ol>
			    <#list intersectionInlinks as link>
                    <li>${link.right}   ${link.left}
                        <a href="/wiki/link/${link.right}">Links</a>
                        <a href="/wiki/article/${link.right}">Detail</a>
                    </li>
			    </#list>
                </ol>

                <h2>Outlink intersection</h2>
                <ol>
				    <#list intersectionOutlinks as link>
                        <li>${link.right}   ${link.left}
                            <a href="/wiki/link/${link.right}">Links</a>
                            <a href="/wiki/article/${link.right}">Detail</a>
                        </li>
				    </#list>
                </ol>
			</#if>
	</div>
</body>
</html>