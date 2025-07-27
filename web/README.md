## 使用 NVM 安装

```shell
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh | bash

nvm install 20
nvm use 20

npm run dev
```

## 更改路由配置

`./src/api/axiosConfig.ts`更新`baseURL`字段为服务器地址

`./src/pages/index.jsx`更新`ws://localhost:8080`字段为服务器地址




