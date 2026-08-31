# ========== 构建阶段 ==========
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 阿里云 Maven 镜像加速（境外网络可删除本段）
RUN mkdir -p /root/.m2 && printf '%s' \
    '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' \
    > /root/.m2/settings.xml

# 先缓存依赖，源码变更时无需重新下载
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# ========== 运行阶段 ==========
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
