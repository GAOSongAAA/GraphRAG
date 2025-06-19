import React from 'react';
import { InboxOutlined } from '@ant-design/icons';
import { App, UploadProps } from 'antd';
import { useMutation } from '@tanstack/react-query';
import { graphRagApi } from '@/api/graphRagApi';
import Dragger from 'antd/es/upload/Dragger';

const FileUploader: React.FC = () => {
  const { message } = App.useApp();

  const uploadMutation = useMutation({
    mutationFn: (file: File) => graphRagApi.uploadDocument(file),
    onSuccess: (response) => {
      if (response.success) {
        message.success(response.data || '文件上传成功!');
      } else {
        message.error(response.message);
      }
    },
    onError: (error) => {
      message.error(`上传失败: ${error.message}`);
    },
  });

  const props: UploadProps = {
    name: 'file',
    multiple: false,
    customRequest: ({ file, onSuccess, onError }) => {
      if (typeof file === 'string') {
        onError?.(new Error('Invalid file type'));
        return;
      }
      uploadMutation
        .mutateAsync(file)
        .then(() => {
          onSuccess?.('ok');
        })
        .catch((err) => {
          onError?.(err);
        });
    },
    maxCount: 1,
  };

  return (
    <Dragger {...props} disabled={uploadMutation.isPending}>
      <p className="ant-upload-drag-icon">
        <InboxOutlined />
      </p>
      <p className="ant-upload-text">点击或拖拽文件到此处上传</p>
      <p className="ant-upload-hint">
        支持单文件上传。后台将自动解析并构建知识图谱。
      </p>
    </Dragger>
  );
};

export default FileUploader;
