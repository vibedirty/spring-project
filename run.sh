#!/usr/bin/env bash

set -u

ms_project_dir="$(cd "$(dirname "$0")" && pwd)"
ms_backend_dir="$ms_project_dir/spring-cloud-backend"

# 新增微服务时，只需追加一行：服务名|端口|相对于项目根目录的 JAR 路径
ms_services=(
  "account-service|8101|spring-cloud-backend/spring-cloud-services/account-service/target/account-service-0.0.1-SNAPSHOT.jar"
  "spring-java-service|8081|spring-cloud-backend/spring-java/target/hard-0.0.1-SNAPSHOT.jar"
  "cart-service|8102|spring-cloud-backend/spring-cloud-services/cart-service/target/cart-service-0.0.1-SNAPSHOT.jar"
  "product-service|8103|spring-cloud-backend/spring-cloud-services/product-service/target/product-service-0.0.1-SNAPSHOT.jar"
  "gateway-service|9000|spring-cloud-backend/spring-cloud-services/gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar"
)

ms_pids=()
ms_names=()
ms_stopping=0

ms_resolve_java() {
  local ms_candidate

  for ms_candidate in \
    "${MS_JAVA_BIN:-}" \
    "${JAVA_HOME:-}/bin/java" \
    "/opt/homebrew/opt/openjdk@17/bin/java" \
    "/usr/local/opt/openjdk@17/bin/java"
  do
    if [ -n "$ms_candidate" ] && [ -x "$ms_candidate" ]; then
      printf '%s\n' "$ms_candidate"
      return 0
    fi
  done

  command -v java 2>/dev/null || return 1
}

ms_build() {
  local ms_java_bin="$1"
  local ms_java_home
  local ms_maven_bin

  ms_maven_bin="$(command -v mvn 2>/dev/null)" || {
    echo "未找到 Maven，请先安装并配置 Maven。" >&2
    return 1
  }
  ms_java_home="$(cd "$(dirname "$ms_java_bin")/.." && pwd)"

  echo "正在打包全部 Java 服务……"
  (
    cd "$ms_backend_dir" || exit 1
    env JAVA_HOME="$ms_java_home" "$ms_maven_bin" -DskipTests package
  )
}

ms_stop_all() {
  local ms_index

  if [ "$ms_stopping" -eq 1 ]; then
    return
  fi
  ms_stopping=1
  trap - EXIT INT TERM HUP

  if [ "${#ms_pids[@]}" -gt 0 ]; then
    echo
    echo "正在停止全部 Java 服务……"
  fi

  for ((ms_index = ${#ms_pids[@]} - 1; ms_index >= 0; ms_index--)); do
    if kill -0 "${ms_pids[$ms_index]}" >/dev/null 2>&1; then
      echo "[${ms_names[$ms_index]}] 停止"
      kill "${ms_pids[$ms_index]}" >/dev/null 2>&1 || true
    fi
  done

  for ms_index in "${!ms_pids[@]}"; do
    wait "${ms_pids[$ms_index]}" 2>/dev/null || true
  done
}

ms_start_all() {
  local ms_java_bin="$1"
  local ms_entry
  local ms_name
  local ms_port
  local ms_jar_relative
  local ms_jar
  local ms_port_pid

  if ! command -v lsof >/dev/null 2>&1; then
    echo "未找到 lsof，无法检查端口占用情况。" >&2
    return 1
  fi

  # 启动前统一检查，避免只启动一部分服务。
  for ms_entry in "${ms_services[@]}"; do
    IFS='|' read -r ms_name ms_port ms_jar_relative <<<"$ms_entry"
    ms_jar="$ms_project_dir/$ms_jar_relative"

    if [ ! -f "$ms_jar" ]; then
      echo "缺少 JAR：$ms_jar" >&2
      echo "请重新执行脚本并选择“重新打包后运行”。" >&2
      return 1
    fi

    ms_port_pid="$(lsof -tiTCP:"$ms_port" -sTCP:LISTEN 2>/dev/null | head -n 1)"
    if [ -n "$ms_port_pid" ]; then
      echo "端口 $ms_port 已被 PID $ms_port_pid 占用，无法启动 $ms_name。" >&2
      return 1
    fi
  done

  trap ms_stop_all EXIT INT TERM HUP

  for ms_entry in "${ms_services[@]}"; do
    IFS='|' read -r ms_name ms_port ms_jar_relative <<<"$ms_entry"
    ms_jar="$ms_project_dir/$ms_jar_relative"

    echo "[$ms_name] 启动，端口=$ms_port"
    env SERVER_PORT="$ms_port" "$ms_java_bin" -jar "$ms_jar" &
    ms_pids+=("$!")
    ms_names+=("$ms_name")
  done

  echo
  echo "${#ms_services[@]} 个服务正在当前窗口运行，按 Ctrl+C 可全部停止。"
  wait
}

ms_java_bin="$(ms_resolve_java)" || {
  echo "未找到 Java，请配置 JDK 17 或设置 MS_JAVA_BIN。" >&2
  exit 1
}

echo "请选择启动方式："
echo "  1) 重新打包后运行"
echo "  2) 直接运行现有 JAR"
echo "  0) 退出"
read -r -p "请输入选项 [1/2/0]：" ms_choice

case "$ms_choice" in
  1)
    ms_build "$ms_java_bin" && ms_start_all "$ms_java_bin"
    ;;
  2)
    ms_start_all "$ms_java_bin"
    ;;
  0)
    echo "已退出。"
    ;;
  *)
    echo "无效选项：$ms_choice" >&2
    exit 1
    ;;
esac
