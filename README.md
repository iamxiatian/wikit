# wikit --- Wikipedia toolkit
An open source toolkit for mining Wikipedia

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


# Reference

Paper:
------------
David Milne, Ian H. Witten. An open-source toolkit for mining Wikipedia. 

Project
--------------
Wikipedia-Miner
