# Stage 1: 官方 GraalVM 社区版（已含 native-image）
FROM ghcr.io/graalvm/native-image-community:latest AS builder

RUN microdnf install -y \
      gcc gcc-c++ make git wget tar gzip which findutils maven \
    && microdnf clean all

# ---------- 构建 musl ----------
ARG MUSL_VER=1.2.5
WORKDIR /tmp
RUN wget -q https://musl.libc.org/releases/musl-${MUSL_VER}.tar.gz \
 && tar -xzf musl-${MUSL_VER}.tar.gz \
 && cd musl-${MUSL_VER} \
 && ./configure --prefix=/opt/musl-${MUSL_VER} --disable-shared \
 && make -j"$(nproc)" \
 && make install \
 && ln -s /opt/musl-${MUSL_VER} /opt/musl \
 && cd / && rm -rf /tmp/musl-${MUSL_VER}*

RUN ln -s /opt/musl/bin/musl-gcc /usr/local/bin/x86_64-linux-musl-gcc \
 && ln -s /opt/musl/bin/musl-gcc /usr/local/bin/x86_64-linux-musl-cc

ENV PATH="/opt/musl/bin:${PATH}"
ENV CC="musl-gcc"
ENV C_INCLUDE_PATH="/opt/musl/include"
ENV LIBRARY_PATH="/opt/musl/lib"

# ---------- 静态 zlib ----------
ARG ZLIB_VERSION=1.3.1
WORKDIR /tmp
RUN wget -q https://zlib.net/zlib-${ZLIB_VERSION}.tar.gz \
 && tar -xzf zlib-${ZLIB_VERSION}.tar.gz \
 && cd zlib-${ZLIB_VERSION} \
 && CC=musl-gcc ./configure --static --prefix=/opt/musl \
 && make -j"$(nproc)" \
 && make install \
 && cd / && rm -rf /tmp/zlib-${ZLIB_VERSION}*

# ---------- Maven 缓存优化 ----------
WORKDIR /app
COPY pom.xml ./

# 先拉依赖并缓存
RUN mvn -B -P native-linux dependency:go-offline

# ---------- 复制源码 ----------
COPY . /app

# ---------- 编译 native image ----------
RUN mvn -P native-linux -DskipTests clean package

# ------------------------------------------------------------
# Stage 2: 输出产物镜像（可以直接 cp 出二进制）
# ------------------------------------------------------------
FROM busybox AS export
WORKDIR /export
COPY --from=builder /app/org.jcnc.snow.cli.SnowCLI /export/Snow