# 内容定位调试策略

## 🎯 当前问题
- **之前**: 内容整体偏右，右侧没有边距，左侧边距过大
- **修复后**: 内容紧贴左边，说明矫枉过正

## 🔍 需要调试的关键点

### 1. WPLayouter中的坐标设置
```java
int dx = pageAttr.leftMargin;  // 当前设置
int dy = pageAttr.topMargin;   // 当前设置
para.setLocation(dx, dy);      // 段落位置设置
```

### 2. LayoutKit中的段落内部偏移
```java
int dx = paraAttr.leftIndent;  // 段落内部的左缩进
```

### 3. 页面视图的缩进设置
```java
pageView.setIndent(pageAttr.leftMargin, pageAttr.topMargin, pageAttr.rightMargin, pageAttr.bottomMargin);
```

## 📊 调试信息输出

### WPLayouter调试
- `WPLayouter: Content positioning` - 显示dx, dy, spanW, spanH和边距
- `WPLayouter: Set paragraph location` - 显示段落实际位置

### LayoutKit调试  
- `LayoutKit: Paragraph width calculation` - 显示段落内部的dx和缩进

## 🎯 可能的问题源头

### 假设1: 多层偏移累积
```
最终位置 = pageView.leftMargin + para.dx + para.leftIndent
```

### 假设2: 坐标系统混乱
- 页面视图坐标系
- 段落坐标系
- 行坐标系

### 假设3: 边距计算错误
- 左右边距不对称
- 边距值计算错误

## 🔧 下一步调试计划
1. 运行测试，查看所有调试输出
2. 分析坐标累积过程
3. 确定正确的坐标设置方案
4. 测试不同的dx/dy组合

## 💡 可能的解决方案
- **方案A**: dx = 0, dy = 0 (页面视图处理所有边距)
- **方案B**: dx = leftMargin, dy = topMargin (当前方案)
- **方案C**: dx = leftMargin/2, dy = topMargin/2 (折中方案)
- **方案D**: 动态调整，根据实际测量结果

目标是找到让内容在页面中居中显示，左右边距平衡的正确设置。