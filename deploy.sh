#!/bin/bash

# 用友数据字典服务部署脚本
# 使用方法: ./deploy.sh [start|stop|restart|logs|status]

APP_NAME="yonyou-datadict"
COMPOSE_FILE="docker-compose.yml"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查Docker和Docker Compose是否安装
check_dependencies() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
}

# 启动服务
start_service() {
    log_info "启动 $APP_NAME 服务..."
    docker-compose -f $COMPOSE_FILE up -d --build
    
    if [ $? -eq 0 ]; then
        log_info "服务启动成功"
        log_info "等待服务就绪..."
        sleep 10
        check_health
    else
        log_error "服务启动失败"
        exit 1
    fi
}

# 停止服务
stop_service() {
    log_info "停止 $APP_NAME 服务..."
    docker-compose -f $COMPOSE_FILE down
    
    if [ $? -eq 0 ]; then
        log_info "服务停止成功"
    else
        log_error "服务停止失败"
        exit 1
    fi
}

# 重启服务
restart_service() {
    log_info "重启 $APP_NAME 服务..."
    stop_service
    start_service
}

# 查看日志
show_logs() {
    log_info "显示 $APP_NAME 服务日志..."
    docker-compose -f $COMPOSE_FILE logs -f
}

# 检查服务状态
check_status() {
    log_info "检查 $APP_NAME 服务状态..."
    docker-compose -f $COMPOSE_FILE ps
}

# 健康检查
check_health() {
    log_info "检查服务健康状态..."
    
    # 等待服务启动
    for i in {1..30}; do
        if curl -f http://localhost:8080/api/actuator/health >/dev/null 2>&1; then
            log_info "服务健康检查通过"
            log_info "服务访问地址: http://localhost:8080/api"
            return 0
        fi
        echo -n "."
        sleep 2
    done
    
    log_warn "健康检查超时，请检查服务状态"
}

# 主函数
main() {
    check_dependencies
    
    case "${1:-start}" in
        start)
            start_service
            ;;
        stop)
            stop_service
            ;;
        restart)
            restart_service
            ;;
        logs)
            show_logs
            ;;
        status)
            check_status
            ;;
        health)
            check_health
            ;;
        *)
            echo "使用方法: $0 {start|stop|restart|logs|status|health}"
            echo "  start   - 启动服务"
            echo "  stop    - 停止服务"
            echo "  restart - 重启服务"
            echo "  logs    - 查看日志"
            echo "  status  - 查看状态"
            echo "  health  - 健康检查"
            exit 1
            ;;
    esac
}

main "$@"