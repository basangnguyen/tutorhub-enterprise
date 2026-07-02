import os
from PIL import Image

dir_path = r"D:\Ban_sao_du_an\src\main\resources\images\DanhLam"
for filename in os.listdir(dir_path):
    if filename.endswith(".jpg"):
        file_path = os.path.join(dir_path, filename)
        try:
            with Image.open(file_path) as img:
                img.load()
                rgb_im = img.convert('RGB')
            rgb_im.save(file_path, 'JPEG')
            print(f"Converted {filename}")
        except Exception as e:
            print(f"Failed to convert {filename}: {e}")
