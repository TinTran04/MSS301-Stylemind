import os
import re

services = ['api-gateway', 'user-service', 'product-service', 'cart-service', 'order-service', 'payment-service', 'notification-service', 'ai-agent-service']

for service in services:
    app_yml_path = f'{service}/src/main/resources/application.yml'
    if os.path.exists(app_yml_path):
        with open(app_yml_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if JWT config exists
        if 'jwt:' in content:
            # Replace JWT paths with defaults
            content = re.sub(
                r'JWT_PUBLIC_KEY_PATH: \${JWT_PUBLIC_KEY_PATH\}',
                'JWT_PUBLIC_KEY_PATH: ${JWT_PUBLIC_KEY_PATH:/app/certs/public_key.pem}',
                content
            )
            content = re.sub(
                r'JWT_PRIVATE_KEY_PATH: \${JWT_PRIVATE_KEY_PATH\}',
                'JWT_PRIVATE_KEY_PATH: ${JWT_PRIVATE_KEY_PATH:/app/certs/private_key.pem}',
                content
            )
            
            with open(app_yml_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f'Updated {app_yml_path}')
        else:
            print(f'No JWT config in {app_yml_path}')
    else:
        print(f'File not found: {app_yml_path}')
