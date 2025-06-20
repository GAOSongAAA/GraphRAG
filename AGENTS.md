# Graph RAG

检视完整源码

# 1. 完整源代码

## 一、目录导航(全部可以修改)

- **graph-rag-common**: 通用组件模块，包含配置、异常处理、工具类等
- **graph-rag-data**: 数据访问模块，包含 Neo4j 实体、仓库、服务等
- **graph-rag-core**: 核心算法模块，包含图遍历、向量检索、答案生成等
- **graph-rag-api**: API 服务模块，包含 REST 接口、监控、限流等

graph-rag-solution/
├── graph-rag-common/          # 通用组件模块
│   ├── src/main/java/
│   │   └── com/graphrag/common/
│   │       ├── config/        # 配置类
│   │       ├── exception/     # 异常定义
│   │       ├── model/         # 通用模型
│   │       └── util/          # 工具类
│   └── pom.xml
├── graph-rag-data/            # 数据访问模块
│   ├── src/main/java/
│   │   └── com/graphrag/data/
│   │       ├── config/        # 数据库配置
│   │       ├── entity/        # 实体类
│   │       ├── repository/    # 数据访问层
│   │       └── service/       # 数据服务层
│   └── pom.xml
├── graph-rag-core/            # 核心算法模块
│   ├── src/main/java/
│   │   └── com/graphrag/core/
│   │       ├── algorithm/     # 核心算法
│   │       ├── model/         # 核心模型
│   │       └── service/       # 核心服务
│   └── pom.xml
├── graph-rag-api/             # API 服务模块
│   ├── src/main/java/
│   │   └── com/graphrag/api/
│   │       ├── config/        # API 配置
│   │       ├── controller/    # 控制器
│   │       ├── interceptor/   # 拦截器
│   │       └── service/       # API 服务
│   └── pom.xml
├── docs/                      # 文档目录
├── docker-compose.yml         # Docker 编排文件
├── Dockerfile                 # Docker 镜像构建文件
└── pom.xml                    # 父级 POM 文件

graphrag-gemini-studio/                           # 前端目录

## 二、任务要求

1. 完全阅读该项目，在graphrag-gemini-studio前端目录文件里面修正API调用格式，要求能在UI界面上反映正确的功能

2. 再度检查API调用，为所有API增加前端接口和画面等资源，同时将问答接口改为异步问答，并且统一将输出变为流式输出，提高反映速度

3. 检查代码是否有问题和bug
