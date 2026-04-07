export interface Media {
  id: string;
  originalFilename: string;
  mimeType: string;
  size: number;
  url: string;
  uploadedBy: string;
  productId?: string;
  createdAt?: string;
}
