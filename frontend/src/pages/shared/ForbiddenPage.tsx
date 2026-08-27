import { Button, Result } from 'antd'
import { Link } from 'react-router-dom'
import { paths } from '@/router/paths'

export function ForbiddenPage() {
  return (
    <Result
      status="403"
      title="无权访问"
      subTitle="当前账号角色不能访问此页面。"
      extra={
        <Button type="primary">
          <Link to={paths.home}>返回首页</Link>
        </Button>
      }
    />
  )
}
