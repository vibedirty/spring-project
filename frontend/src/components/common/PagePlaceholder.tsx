import { Card, Typography } from 'antd'

interface PagePlaceholderProps {
  title: string
  description: string
}

export function PagePlaceholder({ title, description }: PagePlaceholderProps) {
  return (
    <Card className="placeholder-card" bordered={false}>
      <Typography.Title level={2}>{title}</Typography.Title>
      <Typography.Paragraph type="secondary">
        {description}
      </Typography.Paragraph>
    </Card>
  )
}
