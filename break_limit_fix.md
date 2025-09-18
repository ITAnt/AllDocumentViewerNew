# BREAK_LIMIT 逻辑修复

## 🎯 问题诊断
用户反馈：部分行文字碰到右侧尽头才换行，没有保持适当的右边距。

## 🔍 根本原因
**spanW被错误修改导致的BREAK_LIMIT判断错误**：

### 问题流程
1. `layoutLine`开始时：`spanW = w` (例如：69像素)
2. 添加第一个leaf后：`spanW -= leafWidth` (例如：69 - 64 = 5像素)
3. BREAK_LIMIT检查时使用被修改的`spanW`：只剩5像素
4. 右边距计算基于5像素：`maxAllowed = 5 - 3 = 2像素`
5. 结果：强制换行，即使原本有足够空间

### 日志证据
```
// 第一次检查（使用原始宽度）
LayoutKit: Ignoring BREAK_LIMIT - fits with all constraints (h:193, spanW:69, lineW:64, maxAllowed:65)

// 第二次检查（使用被修改的宽度）  
LayoutKit: Keeping BREAK_LIMIT for line wrapping (h:193, spanW:5, lineW:64, maxAllowed:2, margin:3)
```

## ✅ 修复方案

### 1. 保存原始行宽度
```java
int spanW = w;
int originalSpanW = w; // 保存原始行宽度，用于BREAK_LIMIT检查
```

### 2. 使用原始宽度进行BREAK_LIMIT判断
```java
// 修复前（错误）
if (spanW < 100) {
    rightMarginReserve = Math.max(3, spanW / 15);
}
int maxAllowedWidth = spanW - rightMarginReserve;

// 修复后（正确）
if (originalSpanW < 100) {
    rightMarginReserve = Math.max(3, originalSpanW / 15);
}
int maxAllowedWidth = originalSpanW - rightMarginReserve;
```

## 📊 修复效果

### 对于69像素宽的表格单元格
- **修复前**: 使用剩余宽度5px → maxAllowed:2px → 强制换行
- **修复后**: 使用原始宽度69px → maxAllowed:65px → 正常显示

### 对于更宽的容器
- **修复前**: 每个字符都可能触发错误的BREAK_LIMIT判断
- **修复后**: 基于完整行宽度进行合理的换行判断

## 🎯 核心改进
1. **正确的宽度基准**: 使用原始行宽度而不是剩余宽度
2. **一致的判断标准**: 所有leaf使用相同的宽度基准
3. **合理的右边距**: 基于实际可用空间计算边距
4. **减少过度换行**: 避免不必要的强制换行

## 🔧 技术细节
- `spanW`在行布局过程中会递减，反映剩余可用空间
- `originalSpanW`保持不变，反映行的总可用宽度
- BREAK_LIMIT判断应该基于总宽度，而不是剩余宽度
- 这确保了一致的换行策略和合理的右边距

这个修复解决了文字碰到右侧尽头才换行的问题，确保了适当的右边距和更好的文本布局。