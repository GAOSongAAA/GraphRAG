import React from 'react';
import { InboxOutlined } from '@ant-design/icons';
import { App, UploadProps } from 'antd';
import { useMutation } from '@tanstack/react-query';
import { graphRagApi } from '@/api/graphRagApi';
import Dragger from 'antd/es/upload/Dragger';
import { RcFile } from 'antd/es/upload';

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
      message.error(`上传失敗: ${error.message}`);
    },
  });

  const props: UploadProps = {
    name: 'file',
    multiple: false,
    customRequest: ({ file, onSuccess, onError }) => {
      if (typeof file === 'string') {
        onError?.(new Error('無效的文件類型'));
        return;
      }
      // 將 RcFile 轉換為 File 類型
      const rcFile = file as RcFile;
      const convertedFile = new File([rcFile], rcFile.name, { type: rcFile.type, lastModified: rcFile.lastModified });
      
      uploadMutation
        .mutateAsync(convertedFile)
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
      <p className="ant-upload-text">點擊或拖拽文件到此處上傳</p>
      <p className="ant-upload-hint">
        支持單文件上傳。後台將自動解析並構建知識圖譜。
      </p>
    </Dragger>
  );
};

export default FileUploader;
