
### RESERVE IP AUTOMATICALLY 
1. Assigning IP 
```yaml
- name: Create VM with static IP
  google.cloud.gcp_compute_instance:
    name: k8s-master-01
    machine_type: e2-medium
    zone: asia-southeast1-b

    network_interfaces:
      - network:
          name: default
        access_configs:
          - name: External NAT
            nat_ip: "{{ static_ip }}"
            type: ONE_TO_ONE_NAT

    project: "{{ gcp_project }}"
    auth_kind: serviceaccount
    service_account_file: "{{ gcp_credentials }}"
    state: present

```