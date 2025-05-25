# 多阶段构建 - 构建阶段
FROM openjdk:17.0.1-jdk-slim AS builder

# 设置工作目录
WORKDIR /app

# 复制Maven配置文件
COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn

# 复制源代码
COPY src ./src

# 赋予mvnw执行权限并构建应用
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# 运行阶段
FROM openjdk:17.0.1-jdk-slim

# 设置工作目录
WORKDIR /app

# 创建非root用户
RUN addgroup --system spring && adduser --system --group spring

# 从构建阶段复制jar文件
COPY --from=builder /app/target/*.jar app.jar

# 创建日志目录
RUN mkdir /app/logs

# 更改文件和目录所有者
RUN chown spring:spring app.jar && chown spring:spring /app/logs

# 切换到非root用户
USER spring

# 暴露端口
EXPOSE 8080

# 设置JVM参数和启动命令
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "/app/app.jar"]