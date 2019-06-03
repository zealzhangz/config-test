# 多模块打包小记

```
    .
    ├── config
    ├── pom.xml
    └── rest
```
a. config 为不可运行的依赖模块

b. rest 接口模块

c. 将全部依赖打入 `rest Jar` 即可，只有 `rest` 需要打包因此只需要把打包插架放入 `rest` 即可，父模块和依赖模块不放，插件如下：

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
```
此时打出的包为胖 jar ，所有依赖打入一个 jar 中

# 验证依赖模块配置文件读写
当前 `demo` 中的 `config` 模块有依赖文件 `application-config.properties`，试验一下在通过 java 启动指定配置文件是否会覆盖jar中配置文件的值，期望是会覆盖的。

```bash
#nohup java -jar rest-1.0-SNAPSHOT.jar -Dspring.config.location=application.properties > /dev/null 2>&1 &
java -jar rest-1.0-SNAPSHOT.jar -Dspring.config.location=/Users/zealzhangz/Documents/dev/company/sky-test/config-test/rest/target/application.properties
#java -jar rest-1.0-SNAPSHOT.jar -Dspring.config.location=
```
试验发现并不能覆盖，依赖 config-1.0-SNAPSHOT.jar 也被打成了jar 统一放在 `rest/target/BOOT-INF/lib` 下面，包括 Spring 的依赖也在下面。

```
➜  lib git:(master) ✗ ls | grep config 
config-1.0-SNAPSHOT.jar
spring-boot-autoconfigure-1.5.19.RELEASE.jar

```
至这种情况为什么不能覆盖，后续再进行分析。
但是如果再 rest 的配置中加入一个配置，重启后，指定的外部配置的值能覆盖 jar 里面配置。
发现配置是以文件为单位的，比如我把 `config` 模块的配置文件拷贝到和 jar 同级目录，那么这个配置文件就会覆盖 `config` 里面的配置文件。

# 配置文件的加载顺序
## application.yml 和bootStrap.yml
若application.yml 和bootStrap.yml 在同一目录下，则bootStrap.yml 的加载顺序要高于application.yml,即bootStrap.yml  会优先被加载。
原理：
bootstrap.yml 用于应用程序上下文的引导阶段。 bootstrap.yml 由父Spring ApplicationContext加载。

- bootstrap.yml 可以理解成系统级别的一些参数配置，这些参数一般是不会变动的。
- application.yml 可以用来定义应用级别的，如果搭配 spring-cloud-config 使用 application.yml 里面定义的文件可以实现动态替换。

使用Spring Cloud Config Server时，应在 bootstrap.yml 中指定：

```
spring.application.name
spring.cloud.config.server.git.uri
```

### 问题

```
    nested exception is java.lang.IllegalArgumentException: Could not resolve placeholder 'auth.mode' in value "${auth.mode}"
```
原因是因为，`Config Client` 中的 `cloud.config.uri` 应该加在 `bootstrap.yml` 中。程序启动时会首先去`bootstrap.yml` 找 `cloud.config.uri`，找不到就使用默认值 `http://localhost:8888`.

如下有两个解决办法：
1. 新建 `bootstrap.yml` 文件并加入 `cloud.config.uri`，这是最通用的办法，但是要多管理`bootstrap.yml`这个文件，启动指定配置文件时，可能需要指定多个文件（未经实际验证）

    ```bash
        #参考地址（https://stackoverflow.com/questions/25855795/spring-boot-and-multiple-external-configuration-files）
        # 直接指定目录，目录中包含多个配置
        -Dspring.config.location=your/config/dir/
        # 直接指定多个配置
        -Dspring.config.location=classpath:job1.properties,classpath:job2.properties
    ```
2. `Config Server` 服务的地址端口直接使用默认的 `http://localhost:8888`，这样就不用多添加 `bootstrap.yml` 文件，但是当 `Config Server` 地址不能使用localhost，端口不能使用8888时，就比较麻烦，这时最直接的办法是把 `application.yml` 名字改为 `bootstrap.yml` 然后自定义 `cloud.config.uri` 所要的值。这种方法不太通用，建议还是使用方法一。

## 不同位置的配置文件的加载顺序

> SpringApplication will load properties from application.properties files in the following locations and add them to the Spring Environment

1. A /config subdirectory of the current directory
2. The current directory
3. A classpath /config package 
4. The classpath root

### 可以通过属性指定加载某一文件
`java -jar rest-1.0-SNAPSHOT.jar --spring.config.location=classpath:/application.properties,classpath:/test.properties`

当通过spring.config.location 指定一个配置文件时，配置文件的搜索顺序如下：
1. file:./custom-config/
2. classpath:custom-config/
3. file:./config/
4. file:./
5. classpath:/config/
6. classpath:/

最下层的优先加载，所以最上层的属性会覆盖下层的属性；

不管是 jar 包内还是 jar 运行的同级目录下，只要包含 bootstrap.yml，且为云配置，则云配置文件会覆盖其他配置文件；


### 特别说明
在指定配置文件时，尽然没发现 `-Dspring.config.location` 和 `--spring.config.location` 区别，一开始以为两个命令是一样的但是发现用 `-Dspring.config.location` 指定目录后没有生效，后面试用了 `--spring.config.location` 才发现生效正确指定目录对的方法如下：

```bash
➜  test pwd
/Users/zealzhangz/Desktop/test
➜  test ls
application-config.properties  application.properties
# 正确如下✅
java -jar rest-1.0-SNAPSHOT.jar --spring.config.location=file:/Users/zealzhangz/Desktop/test/
# 或如下✅
java -jar rest-1.0-SNAPSHOT.jar --spring.config.location=file:///Users/zealzhangz/Desktop/test/
# 错误如下❌，不能加载到配置
java -jar rest-1.0-SNAPSHOT.jar -Dspring.config.location=file:///Users/zealzhangz/Desktop/test/
```
奇怪的问题：
我单独指定 `application-config.properties` 配置文件发现是不能被加载到的，但是单独指定 `application.properties` 可以加载到里面的内容

```bash
# 不能加载 application-config.properties（这个配置为jar包内部配置）
java -jar rest-1.0-SNAPSHOT.jar --spring.config.location=file:///Users/zealzhangz/Desktop/test/application-config.properties
# 显示的指定两个配置也只能在加载到 application.properties
java -jar rest-1.0-SNAPSHOT.jar --spring.config.location="file:/Users/zealzhangz/Desktop/test/application.properties,file:/Users/zealzhangz/Desktop/test/application-config.properties"
# 上面已经验证，直接指定目录都可以加载到
java -jar rest-1.0-SNAPSHOT.jar --spring.config.location=file:/Users/zealzhangz/Desktop/test/
```