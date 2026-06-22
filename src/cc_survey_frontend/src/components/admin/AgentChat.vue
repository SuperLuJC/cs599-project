<template>
  <div class="agent-chat">
    <!-- 左侧会话列表 -->
    <div class="conversation-list">
      <div class="list-header">
        <h3>AI 助手</h3>
        <el-button type="primary" size="small" @click="createNewConversation">
          <el-icon><Plus /></el-icon>
          新对话
        </el-button>
      </div>

      <el-radio-group v-model="agentType" class="agent-type-selector" @change="handleAgentTypeChange">
        <el-radio-button value="general">通用</el-radio-button>
        <el-radio-button value="survey">问卷</el-radio-button>
        <el-radio-button value="data">数据</el-radio-button>
        <el-radio-button value="log">日志</el-radio-button>
      </el-radio-group>

      <div class="conversations">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: currentConversationId === conv.id }"
          @click="selectConversation(conv.id)"
        >
          <div class="conv-info">
            <div class="conv-title-wrapper">
              <template v-if="conv.editing">
                <el-input
                  v-model="conv.editTitle"
                  size="small"
                  class="title-input"
                  @keyup.enter="saveTitle(conv)"
                  @keyup.esc="cancelEdit(conv)"
                  @blur="saveTitle(conv)"
                />
              </template>
              <template v-else>
                <span class="conv-title-text">{{ conv.title }}</span>
                <el-icon class="edit-icon" @click.stop="startEditTitle(conv)"><Edit /></el-icon>
              </template>
            </div>
            <div class="conv-meta">
              <span class="conv-type">{{ getAgentTypeLabel(conv.agentType) }}</span>
              <span class="conv-time">{{ formatTime(conv.createdAt) }}</span>
            </div>
          </div>
          <el-button
            class="delete-btn"
            circle
            size="small"
            @click.stop="deleteConversation(conv.id)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-area">
      <div class="messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-state">
          <el-icon :size="48"><ChatDotRound /></el-icon>
          <p>开始与 AI 助手对话</p>
          <p class="hint">选择助手类型开始对话</p>
        </div>

        <div
          v-for="msg in messages"
          :key="msg.id || msg.createdAt"
          class="message"
          :class="msg.role"
        >
          <div class="message-avatar">
            <el-avatar v-if="msg.role === 'user'" :size="32">
              {{ authStore.name?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <el-avatar v-else :size="32" class="ai-avatar">
              AI
            </el-avatar>
          </div>
          <div class="message-content">
            <div class="message-text" v-html="renderMarkdown(msg.content)"></div>
          </div>
        </div>

        <div v-if="loading" class="message ai loading">
          <div class="message-avatar">
            <el-avatar :size="32" class="ai-avatar">AI</el-avatar>
          </div>
          <div class="message-content">
            <div class="streaming-content">{{ streamingContent }}</div>
            <span class="cursor-blink">|</span>
          </div>
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="输入消息... (Ctrl+Enter 发送)"
          @keydown.enter.ctrl="sendMessage"
          :disabled="loading"
        />
        <el-button
          type="primary"
          @click="sendMessage"
          :loading="loading"
          :disabled="!inputMessage.trim()"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { Plus, Delete, ChatDotRound, Edit } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'

const authStore = useAuthStore()
const md = new MarkdownIt()

const agentType = ref('general')
const conversations = ref([])
const currentConversationId = ref(null)
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const streamingContent = ref('')
const messagesContainer = ref(null)
let messageIdCounter = 0  // 用于生成唯一消息 ID

const agentTypeLabels = {
  general: '通用助手',
  survey: '问卷助手',
  data: '数据助手',
  log: '日志助手'
}

function getAgentTypeLabel(type) {
  return agentTypeLabels[type] || type
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString()
}

function renderMarkdown(content) {
  if (!content) return ''
  return md.render(content)
}

async function loadConversations() {
  try {
    const res = await fetch('/api/agent/conversations', {
      credentials: 'include'
    })
    const data = await res.json()
    conversations.value = data.data || []
  } catch (e) {
    console.error('Failed to load conversations:', e)
  }
}

async function selectConversation(id) {
  currentConversationId.value = id
  try {
    const res = await fetch(`/api/agent/conversations/${id}/messages`, {
      credentials: 'include'
    })
    const data = await res.json()
    messages.value = data.data || []
    scrollToBottom()
  } catch (e) {
    console.error('Failed to load messages:', e)
  }
}

function createNewConversation() {
  currentConversationId.value = null
  messages.value = []
}

function handleAgentTypeChange() {
  createNewConversation()
}

async function sendMessage() {
  if (!inputMessage.value.trim() || loading.value) return

  const message = inputMessage.value.trim()
  inputMessage.value = ''

  // 添加用户消息到显示
  messages.value.push({
    id: ++messageIdCounter,
    role: 'user',
    content: message,
    createdAt: new Date().toISOString()
  })
  scrollToBottom()

  loading.value = true
  streamingContent.value = ''

  try {
    // 使用流式 API
    const response = await fetch('/api/agent/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify({
        conversationId: currentConversationId.value,
        agentType: agentType.value,
        message: message
      })
    })

    // 检查响应状态
    if (!response.ok) {
      const errorData = await response.json()
      throw new Error(errorData.message || '请求失败')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''  // 缓冲区用于处理不完整的数据

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')

      // 保留最后一个可能不完整的行
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          try {
            const jsonStr = line.substring(5).trim()
            if (jsonStr) {
              const event = JSON.parse(jsonStr)
              handleStreamEvent(event)
            }
          } catch (e) {
            console.warn('Failed to parse SSE event:', line, e)
          }
        }
      }
    }

    // 处理缓冲区中剩余的数据
    if (buffer.startsWith('data:')) {
      try {
        const jsonStr = buffer.substring(5).trim()
        if (jsonStr) {
          const event = JSON.parse(jsonStr)
          handleStreamEvent(event)
        }
      } catch (e) {
        // 忽略解析错误
      }
    }

    // 刷新会话列表
    await loadConversations()
  } catch (e) {
    console.error('Failed to send message:', e)
    messages.value.push({
      id: ++messageIdCounter,
      role: 'ai',
      content: `抱歉，服务暂时不可用：${e.message}`,
      createdAt: new Date().toISOString()
    })
    loading.value = false
    streamingContent.value = ''
  } finally {
    scrollToBottom()
  }
}

function handleStreamEvent(event) {
  switch (event.type) {
    case 'conversation_created':
      currentConversationId.value = event.conversationId
      break
    case 'title':
      // 更新会话列表中的标题
      const conv = conversations.value.find(c => c.id === currentConversationId.value)
      if (conv) {
        conv.title = event.data
      }
      break
    case 'content':
      streamingContent.value += event.data
      scrollToBottom()
      break
    case 'done':
      // 先设置 loading 为 false，让流式消息区域消失
      loading.value = false
      // 将流式内容添加到消息列表
      if (streamingContent.value) {
        messages.value.push({
          id: ++messageIdCounter,
          role: 'ai',  // 使用 'ai' 与后端保持一致
          content: streamingContent.value,
          createdAt: new Date().toISOString()
        })
        streamingContent.value = ''
        scrollToBottom()
      }
      break
    case 'error':
      loading.value = false
      streamingContent.value = ''
      messages.value.push({
        id: ++messageIdCounter,
        role: 'ai',
        content: `错误: ${event.data}`,
        createdAt: new Date().toISOString()
      })
      break
  }
}

async function deleteConversation(id) {
  try {
    await fetch(`/api/agent/conversations/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    conversations.value = conversations.value.filter(c => c.id !== id)
    if (currentConversationId.value === id) {
      createNewConversation()
    }
  } catch (e) {
    console.error('Failed to delete conversation:', e)
  }
}

// 开始编辑标题
function startEditTitle(conv) {
  conv.editing = true
  conv.editTitle = conv.title
}

// 保存标题
async function saveTitle(conv) {
  // 如果正在编辑但没有 editTitle，取消编辑
  if (!conv.editing) return

  const newTitle = conv.editTitle?.trim() || ''

  // 如果标题为空或未改变，取消编辑
  if (newTitle === '' || newTitle === conv.title) {
    cancelEdit(conv)
    return
  }

  try {
    const res = await fetch(`/api/agent/conversations/${conv.id}/title`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ title: newTitle })
    })
    const data = await res.json()
    if (data.code === 200) {
      conv.title = newTitle
    }
    conv.editing = false
    conv.editTitle = ''
  } catch (e) {
    console.error('Failed to update title:', e)
    cancelEdit(conv)
  }
}

// 取消编辑
function cancelEdit(conv) {
  conv.editing = false
  conv.editTitle = ''
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

onMounted(() => {
  loadConversations()
})
</script>

<style scoped lang="scss">
.agent-chat {
  display: flex;
  height: 100%;
  background: #fff;
}

.conversation-list {
  width: 280px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;

  .list-header {
    padding: 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e4e7ed;

    h3 {
      font-size: 16px;
      color: #303133;
    }
  }

  .agent-type-selector {
    padding: 12px 16px;
    border-bottom: 1px solid #e4e7ed;

    :deep(.el-radio-button__inner) {
      padding: 8px 12px;
      font-size: 12px;
    }
  }

  .conversations {
    flex: 1;
    overflow-y: auto;
    padding: 8px;

    .conversation-item {
      padding: 12px;
      border-radius: 8px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: space-between;
      transition: background 0.2s;

      &:hover {
        background: #f5f7fa;
      }

      &.active {
        background: #ecf5ff;
      }

      .conv-info {
        flex: 1;
        min-width: 0;

        .conv-title-wrapper {
          display: flex;
          align-items: center;
          gap: 4px;

          .conv-title-text {
            font-size: 14px;
            color: #303133;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            flex: 1;
          }

          .edit-icon {
            opacity: 0;
            font-size: 12px;
            color: #909399;
            cursor: pointer;
            flex-shrink: 0;

            &:hover {
              color: #409eff;
            }
          }

          .title-input {
            width: 100%;
          }
        }

        .conv-meta {
          display: flex;
          gap: 8px;
          margin-top: 4px;

          .conv-type {
            font-size: 12px;
            color: #909399;
          }

          .conv-time {
            font-size: 12px;
            color: #c0c4cc;
          }
        }
      }

      &:hover .edit-icon {
        opacity: 1;
      }

      .delete-btn {
        opacity: 0;
        transition: opacity 0.2s;
        border: none;
        background: transparent;

        &:hover {
          background: #f5f7fa;
        }
      }

      &:hover .delete-btn {
        opacity: 1;
      }
    }
  }
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;

  .messages {
    flex: 1;
    overflow-y: auto;
    padding: 16px;

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      color: #909399;

      p {
        margin-top: 16px;
        font-size: 16px;
      }

      .hint {
        font-size: 14px;
        color: #c0c4cc;
      }
    }

    .message {
      display: flex;
      gap: 12px;
      margin-bottom: 16px;

      &.user {
        .message-content {
          background: #ecf5ff;
        }
      }

      &.ai {
        .message-content {
          background: #f5f7fa;
        }
      }

      .message-avatar {
        .ai-avatar {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: #fff;
        }
      }

      .message-content {
        flex: 1;
        padding: 12px 16px;
        border-radius: 8px;
        max-width: 80%;

        .message-text {
          font-size: 14px;
          line-height: 1.6;
          color: #303133;

          :deep(p) {
            margin: 0 0 8px;
          }

          :deep(ul), :deep(ol) {
            margin: 8px 0;
            padding-left: 20px;
          }

          :deep(code) {
            background: #e4e7ed;
            padding: 2px 6px;
            border-radius: 4px;
            font-family: monospace;
          }

          :deep(pre) {
            background: #f5f7fa;
            padding: 12px;
            border-radius: 8px;
            overflow-x: auto;
          }

          :deep(strong) {
            font-weight: 600;
          }
        }

        .streaming-content {
          font-size: 14px;
          line-height: 1.6;
          color: #303133;
          white-space: pre-wrap;
          word-wrap: break-word;
        }

        .cursor-blink {
          animation: blink 1s infinite;
          color: #409eff;
        }
      }
    }
  }

  .input-area {
    padding: 16px;
    border-top: 1px solid #e4e7ed;
    display: flex;
    gap: 12px;

    .el-textarea {
      flex: 1;
    }
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>