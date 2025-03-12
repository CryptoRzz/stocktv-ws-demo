# StockTV WS 测试案例

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-8%2B-orange)](https://www.oracle.com/java/)

StockTV WS 的 Java 客户端实现，提供股票实时数据的推送功能，附带前端测试文件。


### 心跳机制

```java
try {
    // 发送心跳（心跳内容可自定义）
    session.sendMessage(new TextMessage("heart"));
} catch (IOException e) {
    log.error("发送心跳失败：{}", e.getMessage());
}
```

### 重连机制
```java
try {
    // 重新连接
    MyWebSocketClient.this.reconnect();
} catch (Exception e) {
    log.error("重连失败：{}", e.getMessage());
}
```
---

**技术支持**：访问 [StockTV 官方文档](https://stocktv.top/) 或 [联系StockTV客服](https://t.me/CryptoRzz)