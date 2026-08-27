import { Button, Result } from 'antd'
import { Link } from 'react-router-dom'
import { paths } from '@/router/paths'

export function NotFoundPage() {
  return (
    <Result
      status="404"
      title="页面不存在"
      subTitle="请检查访问地址。"
      extra={
        <Button type="primary">
          <Link to={paths.home}>返回首页</Link>
        </Button>
      }
    />
  )
}
