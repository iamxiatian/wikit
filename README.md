# wikit --- Wikipedia toolkit
一个开源的维基百科挖掘分析工具，集成了维基百科数据的解析、变换、存储和部分语义分析功能，已经实现的功能有：

1. 显性语义分析(clone 来源待补充)
2. 层次语义路径识别/显性语义路径挖掘
3. 链接相似度计算
4. 词语位置加权TextRank的关键词抽取、融合维基百科链接信息的关键词抽取(@see KeywordExtractor)

# 如何运行
下面仅以Ubuntu系统为例，说明项目如何运行。

1. 确保系统中已经安装Oracle JDK 1.8以上版本
2. 运行如下命令安装sdkman：

        $ curl -s http://get.sdkman.io | bash
3. 利用sdkman安装gradle, 也可以不安装sdkman，自己手工安装gradle

        $ sdkman install gradle
4. 取出源代码
5. 在Terminal中进入wikit目录
6. 编译代码

        $ gradle compileJava
        
        $ gradle copyJars
7. 利用run.py 运行指定类，例如

        $ ./run.py ESAModel -c expt/conf/chinese.xml

#怎么构建ESA模型(以中文数据为例)
1. 下载维基百科导出数据，如zhwiki-20150602-pages-articles-multistream
2. 修改expt/conf/conf-chinese.xml,调整里面的文件路径参数，假设数据都保存在~/esa/chinese目录下
3. 运行redis-server
4. 编译代码后运行PageSequenceDump，过滤低质量的维基数据，形成新的数据文件

        $./run.py PageSequenceDump -c expt/conf/conf-chinese.xml -ba -split
        
   运行完毕后会在配置的conf-chinese.xml中配置的地方生成seq-article.gz和seq-category.gz两个文件
        
5. 训练生成ESA模型

        $./run.py ESAModelBuilder -c expt/conf/conf-chinese.xml -build
        
6. 测试ESA模型
        
        $./run.py ESAModel -c expt/conf/conf-chinese.xml -lookup
        
7. 建立维基百科文章、类别对象的标题和ID的双向映射关系
        
        $./run.py WikiNameIdMapper -c expt/conf/conf-chinese.xml -build
        
8. 处理维基百科文章的重定向、类别关系

        $./run.py ArticleCache -c expt/conf/conf-chinese.xml -build

7. 链接数据库的构建: 构建维基页面的入链和出链关系，为WLM相关度计算准备数据

        $./run.py LinkCache -c expt/conf/conf-chinese.xml -build
        
        
# Reference

Paper:
------------
1. David Milne, Ian H. Witten. An open-source toolkit for mining Wikipedia.

2. 夏天. 词语位置加权TextRank的关键词抽取研究. 现代图书情报技术, 2013, 29(9): 30-34.


Project
--------------
Wikipedia-Miner
JWPL: https://www.ukp.tu-darmstadt.de/software/jwpl/
Mallet: http://mallet.cs.umass.edu/ (update trove4j library to version 3.0.3)
