import os

with open('app/src/main/assets/mascot_3d/three.min.js', 'r', encoding='utf-8') as f:
    three_js = f.read()

scene_js = """
    const container = document.getElementById('canvas-container');
    const scene = new THREE.Scene();

    const camera = new THREE.PerspectiveCamera(38, 1.0, 0.1, 100);
    camera.position.set(0, 0.2, 5.2);

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, powerPreference: 'high-performance' });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 2, 2));
    renderer.setClearColor(0x000000, 0); // 100% Transparent
    renderer.outputEncoding = THREE.sRGBEncoding;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.15;
    container.appendChild(renderer.domElement);

    function updateSize() {
        const w = container.clientWidth || window.innerWidth || 300;
        const h = container.clientHeight || window.innerHeight || 300;
        if (w > 10 && h > 10) {
            camera.aspect = w / h;
            camera.updateProjectionMatrix();
            renderer.setSize(w, h);
        }
    }
    updateSize();

    if (window.ResizeObserver) {
        const ro = new ResizeObserver(() => { updateSize(); });
        ro.observe(container);
    }
    window.addEventListener('resize', updateSize);
    setTimeout(updateSize, 50);
    setTimeout(updateSize, 200);
    setTimeout(updateSize, 600);

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.95);
    scene.add(ambientLight);

    const keyLight = new THREE.DirectionalLight(0xfffaed, 1.6);
    keyLight.position.set(4, 7, 5);
    scene.add(keyLight);

    const fillLight = new THREE.DirectionalLight(0x38bdf8, 1.1);
    fillLight.position.set(-5, 1, 3);
    scene.add(fillLight);

    const rimLight = new THREE.DirectionalLight(0x4ade80, 1.4);
    rimLight.position.set(0, -3, -5);
    scene.add(rimLight);

    const slimePointLight = new THREE.PointLight(0x22c55e, 1.5, 6);
    slimePointLight.position.set(0, 0.2, 1.2);
    scene.add(slimePointLight);

    const ghostPointLight = new THREE.PointLight(0x38bdf8, 1.4, 5);
    scene.add(ghostPointLight);

    const mascotGroup = new THREE.Group();
    scene.add(mascotGroup);

    const bones = [];
    const rootBone = new THREE.Bone();
    rootBone.position.y = -1.1;
    bones.push(rootBone);

    const spineBone = new THREE.Bone();
    spineBone.position.y = 0.75;
    rootBone.add(spineBone);
    bones.push(spineBone);

    const headBone = new THREE.Bone();
    headBone.position.y = 0.75;
    spineBone.add(headBone);
    bones.push(headBone);

    const leftArmBone = new THREE.Bone();
    leftArmBone.position.set(-0.95, -0.15, 0.1);
    spineBone.add(leftArmBone);
    bones.push(leftArmBone);

    const rightArmBone = new THREE.Bone();
    rightArmBone.position.set(0.95, -0.15, 0.1);
    spineBone.add(rightArmBone);
    bones.push(rightArmBone);

    const skeleton = new THREE.Skeleton(bones);

    const slimeGeo = new THREE.SphereGeometry(1.22, 48, 48);
    const pos = slimeGeo.attributes.position;
    const skinIndices = [];
    const skinWeights = [];

    for (let i = 0; i < pos.count; i++) {
        let x = pos.getX(i);
        let y = pos.getY(i);
        let z = pos.getZ(i);

        if (y < 0) {
            const factor = 1.0 + Math.abs(y) * 0.38;
            pos.setX(i, x * factor);
            pos.setZ(i, z * factor);
            pos.setY(i, y * 0.88);
        } else {
            const factor = 1.0 - y * 0.14;
            pos.setX(i, x * factor);
            pos.setZ(i, z * factor);
        }

        const normY = (y + 1.2) / 2.4;
        if (normY < 0.35) {
            skinIndices.push(0, 1, 0, 0);
            skinWeights.push(1.0 - normY / 0.35, normY / 0.35, 0, 0);
        } else if (normY < 0.7) {
            const t = (normY - 0.35) / 0.35;
            skinIndices.push(1, 2, 0, 0);
            skinWeights.push(1.0 - t, t, 0, 0);
        } else {
            skinIndices.push(2, 1, 0, 0);
            skinWeights.push(0.92, 0.08, 0, 0);
        }
    }

    slimeGeo.computeVertexNormals();
    slimeGeo.setAttribute('skinIndex', new THREE.Uint16BufferAttribute(skinIndices, 4));
    slimeGeo.setAttribute('skinWeight', new THREE.Float32BufferAttribute(skinWeights, 4));

    const slimeMat = new THREE.MeshPhysicalMaterial({
        color: 0x22c55e,
        emissive: 0x052e16,
        emissiveIntensity: 0.25,
        roughness: 0.1,
        metalness: 0.02,
        clearcoat: 1.0,
        clearcoatRoughness: 0.08,
        transmission: 0.42,
        ior: 1.45,
        reflectivity: 0.9,
        transparent: true,
        opacity: 0.96
    });

    const slimeMesh = new THREE.SkinnedMesh(slimeGeo, slimeMat);
    slimeMesh.add(rootBone);
    slimeMesh.bind(skeleton);
    mascotGroup.add(slimeMesh);

    const coreGeo = new THREE.SphereGeometry(0.75, 24, 24);
    const coreMat = new THREE.MeshBasicMaterial({
        color: 0x4ade80,
        transparent: true,
        opacity: 0.45
    });
    const coreMesh = new THREE.Mesh(coreGeo, coreMat);
    coreMesh.position.y = -0.1;
    spineBone.add(coreMesh);

    const armGeo = new THREE.SphereGeometry(0.32, 24, 24);
    armGeo.scale(1.5, 0.7, 0.7);

    const leftArmMesh = new THREE.Mesh(armGeo, slimeMat);
    leftArmMesh.position.set(-0.22, 0, 0);
    leftArmBone.add(leftArmMesh);

    const rightArmMesh = new THREE.Mesh(armGeo, slimeMat);
    rightArmMesh.position.set(0.22, 0, 0);
    rightArmBone.add(rightArmMesh);

    const faceGroup = new THREE.Group();
    headBone.add(faceGroup);
    faceGroup.position.set(0, -0.38, 0.92);

    const eyeGroup = new THREE.Group();
    faceGroup.add(eyeGroup);

    const scleraGeo = new THREE.SphereGeometry(0.26, 28, 28);
    scleraGeo.scale(1, 1.18, 0.7);
    const scleraMat = new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.08 });

    const pupilGeo = new THREE.SphereGeometry(0.14, 24, 24);
    pupilGeo.scale(1, 1.1, 0.35);
    const pupilMat = new THREE.MeshStandardMaterial({ color: 0x064e3b, roughness: 0.1 });

    const innerPupilGeo = new THREE.SphereGeometry(0.09, 20, 20);
    innerPupilGeo.scale(1, 1.1, 0.4);
    const innerPupilMat = new THREE.MeshBasicMaterial({ color: 0x022c22 });

    const glintMainGeo = new THREE.SphereGeometry(0.05, 14, 14);
    const glintSubGeo = new THREE.SphereGeometry(0.025, 10, 10);
    const glintMat = new THREE.MeshBasicMaterial({ color: 0xffffff });

    const leftEye = new THREE.Group();
    leftEye.position.set(-0.38, 0.14, 0);
    const leftSclera = new THREE.Mesh(scleraGeo, scleraMat);
    const leftIris = new THREE.Mesh(pupilGeo, pupilMat);
    leftIris.position.set(0.03, 0.02, 0.17);
    const leftPupil = new THREE.Mesh(innerPupilGeo, innerPupilMat);
    leftPupil.position.set(0.03, 0.02, 0.19);
    const leftGlint1 = new THREE.Mesh(glintMainGeo, glintMat);
    leftGlint1.position.set(0.06, 0.07, 0.22);
    const leftGlint2 = new THREE.Mesh(glintSubGeo, glintMat);
    leftGlint2.position.set(0.01, -0.02, 0.22);
    leftEye.add(leftSclera, leftIris, leftPupil, leftGlint1, leftGlint2);
    eyeGroup.add(leftEye);

    const rightEye = new THREE.Group();
    rightEye.position.set(0.38, 0.14, 0);
    const rightSclera = new THREE.Mesh(scleraGeo, scleraMat);
    const rightIris = new THREE.Mesh(pupilGeo, pupilMat);
    rightIris.position.set(-0.03, 0.02, 0.17);
    const rightPupil = new THREE.Mesh(innerPupilGeo, innerPupilMat);
    rightPupil.position.set(-0.03, 0.02, 0.19);
    const rightGlint1 = new THREE.Mesh(glintMainGeo, glintMat);
    rightGlint1.position.set(-0.01, 0.07, 0.22);
    const rightGlint2 = new THREE.Mesh(glintSubGeo, glintMat);
    rightGlint2.position.set(-0.05, -0.02, 0.22);
    rightEye.add(rightSclera, rightIris, rightPupil, rightGlint1, rightGlint2);
    eyeGroup.add(rightEye);

    const smileCurve = new THREE.QuadraticBezierCurve3(
        new THREE.Vector3(-0.32, -0.12, 0),
        new THREE.Vector3(0, -0.38, 0.05),
        new THREE.Vector3(0.32, -0.12, 0)
    );
    const smileGeo = new THREE.TubeGeometry(smileCurve, 24, 0.042, 12, false);
    const smileMat = new THREE.MeshStandardMaterial({ color: 0x052e16, roughness: 0.2 });
    const smileMesh = new THREE.Mesh(smileGeo, smileMat);
    smileMesh.position.set(0, 0.04, 0.16);
    faceGroup.add(smileMesh);

    const toothGeo = new THREE.ConeGeometry(0.04, 0.07, 10);
    toothGeo.rotateX(Math.PI);
    const toothMat = new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.1 });
    const leftTooth = new THREE.Mesh(toothGeo, toothMat);
    leftTooth.position.set(-0.12, -0.14, 0.21);
    const rightTooth = new THREE.Mesh(toothGeo, toothMat);
    rightTooth.position.set(0.12, -0.14, 0.21);
    faceGroup.add(leftTooth, rightTooth);

    const ghostGroup = new THREE.Group();
    scene.add(ghostGroup);

    const ghostHeadGeo = new THREE.SphereGeometry(0.40, 28, 28);
    const ghostSkirtGeo = new THREE.ConeGeometry(0.40, 0.65, 28, 4, true);
    ghostSkirtGeo.rotateX(Math.PI);
    ghostSkirtGeo.translate(0, -0.22, 0);

    const ghostMat = new THREE.MeshStandardMaterial({
        color: 0xffffff,
        emissive: 0x38bdf8,
        emissiveIntensity: 0.65,
        roughness: 0.15,
        transparent: true,
        opacity: 0.94
    });

    const ghostHead = new THREE.Mesh(ghostHeadGeo, ghostMat);
    const ghostSkirt = new THREE.Mesh(ghostSkirtGeo, ghostMat);
    ghostGroup.add(ghostHead, ghostSkirt);

    const ghostEyeGeo = new THREE.SphereGeometry(0.055, 14, 14);
    const ghostEyeMat = new THREE.MeshBasicMaterial({ color: 0x0369a1 });
    const gLeftEye = new THREE.Mesh(ghostEyeGeo, ghostEyeMat);
    gLeftEye.position.set(-0.12, 0.05, 0.36);
    const gRightEye = new THREE.Mesh(ghostEyeGeo, ghostEyeMat);
    gRightEye.position.set(0.12, 0.05, 0.36);
    ghostGroup.add(gLeftEye, gRightEye);

    const blushGeo = new THREE.SphereGeometry(0.06, 12, 12);
    blushGeo.scale(1.4, 0.6, 0.3);
    const blushMat = new THREE.MeshBasicMaterial({ color: 0xf472b6, transparent: true, opacity: 0.65 });
    const gLeftBlush = new THREE.Mesh(blushGeo, blushMat);
    gLeftBlush.position.set(-0.18, -0.04, 0.34);
    const gRightBlush = new THREE.Mesh(blushGeo, blushMat);
    gRightBlush.position.set(0.18, -0.04, 0.34);
    ghostGroup.add(gLeftBlush, gRightBlush);

    const gArmGeo = new THREE.SphereGeometry(0.1, 14, 14);
    gArmGeo.scale(0.6, 1.4, 0.6);
    const gLeftArm = new THREE.Mesh(gArmGeo, ghostMat);
    gLeftArm.position.set(-0.35, -0.08, 0.1);
    gLeftArm.rotation.z = Math.PI * 0.25;
    const gRightArm = new THREE.Mesh(gArmGeo, ghostMat);
    gRightArm.position.set(0.35, -0.08, 0.1);
    gRightArm.rotation.z = -Math.PI * 0.25;
    ghostGroup.add(gLeftArm, gRightArm);

    const particleCount = 28;
    const particleGeo = new THREE.BufferGeometry();
    const particlePositions = new Float32Array(particleCount * 3);

    for (let i = 0; i < particleCount * 3; i += 3) {
        particlePositions[i] = (Math.random() - 0.5) * 3.5;
        particlePositions[i + 1] = (Math.random() - 0.5) * 3.0 + 0.2;
        particlePositions[i + 2] = (Math.random() - 0.5) * 2.5;
    }
    particleGeo.setAttribute('position', new THREE.BufferAttribute(particlePositions, 3));

    const particleMat = new THREE.PointsMaterial({
        color: 0x4ade80,
        size: 0.07,
        transparent: true,
        opacity: 0.65,
        blending: THREE.AdditiveBlending
    });
    const particles = new THREE.Points(particleGeo, particleMat);
    scene.add(particles);

    const shadowGeo = new THREE.PlaneGeometry(3.6, 3.6);
    const shadowCanvas = document.createElement('canvas');
    shadowCanvas.width = 128;
    shadowCanvas.height = 128;
    const sCtx = shadowCanvas.getContext('2d');
    const gradient = sCtx.createRadialGradient(64, 64, 0, 64, 64, 60);
    gradient.addColorStop(0, 'rgba(0,0,0,0.60)');
    gradient.addColorStop(0.45, 'rgba(0,0,0,0.20)');
    gradient.addColorStop(1, 'rgba(0,0,0,0)');
    sCtx.fillStyle = gradient;
    sCtx.fillRect(0, 0, 128, 128);

    const shadowTex = new THREE.CanvasTexture(shadowCanvas);
    const shadowMat = new THREE.MeshBasicMaterial({
        map: shadowTex,
        transparent: true,
        opacity: 0.85,
        depthWrite: false
    });
    const shadowMesh = new THREE.Mesh(shadowGeo, shadowMat);
    shadowMesh.rotation.x = -Math.PI / 2;
    shadowMesh.position.y = -1.55;
    scene.add(shadowMesh);

    let currentState = 'idle';
    let tapJumpTimer = 0;
    let blinkTimer = 0;
    let isBlinking = false;
    let clock = new THREE.Clock();

    let isDragging = false;
    let prevPointerX = 0;
    let prevPointerY = 0;
    let targetRotY = 0;
    let targetRotX = 0;
    let currentRotY = 0;
    let currentRotX = 0;

    window.addEventListener('pointerdown', (e) => {
        isDragging = true;
        prevPointerX = e.clientX;
        prevPointerY = e.clientY;
        triggerTap();
    });

    window.addEventListener('pointermove', (e) => {
        if (!isDragging) return;
        const dx = e.clientX - prevPointerX;
        const dy = e.clientY - prevPointerY;
        prevPointerX = e.clientX;
        prevPointerY = e.clientY;

        targetRotY += dx * 0.013;
        targetRotX += dy * 0.011;
        targetRotX = Math.max(-0.55, Math.min(0.55, targetRotX));
    });

    window.addEventListener('pointerup', () => { isDragging = false; });
    window.addEventListener('pointercancel', () => { isDragging = false; });

    function triggerTap() {
        tapJumpTimer = 1.0;
        try {
            if (window.AndroidBridge && window.AndroidBridge.onMascotTapped) {
                window.AndroidBridge.onMascotTapped();
            }
        } catch (_) {}
    }

    window.setMascotState = function(newState) {
        currentState = newState || 'idle';
        if (currentState === 'overTarget') {
            slimeMat.color.setHex(0xef4444);
            slimePointLight.color.setHex(0xef4444);
            particleMat.color.setHex(0xef4444);
        } else if (currentState === 'party') {
            slimeMat.color.setHex(0xffd600);
            slimePointLight.color.setHex(0xffd600);
            ghostMat.emissive.setHex(0xf43f5e);
            particleMat.color.setHex(0xffd600);
        } else if (currentState === 'typing') {
            slimeMat.color.setHex(0x38bdf8);
            slimePointLight.color.setHex(0x38bdf8);
            particleMat.color.setHex(0x38bdf8);
        } else {
            slimeMat.color.setHex(0x22c55e);
            slimePointLight.color.setHex(0x22c55e);
            ghostMat.emissive.setHex(0x38bdf8);
            particleMat.color.setHex(0x4ade80);
        }
    };

    window.triggerTap = triggerTap;

    function animate() {
        requestAnimationFrame(animate);
        const elapsedTime = clock.getElapsedTime();

        // Ensure canvas is never 0x0
        if (renderer.domElement.width < 10 || isNaN(camera.aspect)) {
            updateSize();
        }

        if (!isDragging) {
            targetRotY *= 0.93;
            targetRotX *= 0.93;
        }
        currentRotY += (targetRotY - currentRotY) * 0.13;
        currentRotX += (targetRotX - currentRotX) * 0.13;

        mascotGroup.rotation.y = currentRotY;
        mascotGroup.rotation.x = currentRotX;

        const breath = Math.sin(elapsedTime * 2.8) * 0.07;
        const sway = Math.sin(elapsedTime * 2.0) * 0.08;

        spineBone.position.y = 0.75 + breath;
        spineBone.rotation.z = sway;
        headBone.rotation.z = -sway * 0.65;
        headBone.rotation.x = Math.sin(elapsedTime * 1.5) * 0.05;

        blinkTimer += 0.016;
        if (blinkTimer > 3.8) {
            isBlinking = true;
            eyeGroup.scale.y = Math.max(0.08, eyeGroup.scale.y - 0.25);
            if (eyeGroup.scale.y <= 0.1) {
                blinkTimer = 0;
            }
        } else if (isBlinking) {
            eyeGroup.scale.y = Math.min(1.0, eyeGroup.scale.y + 0.25);
            if (eyeGroup.scale.y >= 1.0) {
                isBlinking = false;
            }
        }

        if (currentState === 'party') {
            leftArmBone.rotation.z = Math.PI * 0.65 + Math.sin(elapsedTime * 8.0) * 0.4;
            rightArmBone.rotation.z = -Math.PI * 0.65 - Math.sin(elapsedTime * 8.0) * 0.4;
            headBone.rotation.y = Math.sin(elapsedTime * 6.0) * 0.35;
        } else if (currentState === 'typing') {
            leftArmBone.rotation.z = Math.PI * 0.22 + Math.sin(elapsedTime * 12.0) * 0.25;
            rightArmBone.rotation.z = -Math.PI * 0.22 - Math.cos(elapsedTime * 12.0) * 0.25;
        } else {
            leftArmBone.rotation.z = Math.PI * 0.25 + Math.sin(elapsedTime * 2.5) * 0.12;
            rightArmBone.rotation.z = -Math.PI * 0.25 - Math.sin(elapsedTime * 2.5) * 0.12;
        }

        if (tapJumpTimer > 0) {
            tapJumpTimer -= 0.042;
            const jumpPhase = Math.sin((1.0 - tapJumpTimer) * Math.PI);
            mascotGroup.position.y = jumpPhase * 0.72;
            mascotGroup.scale.set(1.0 - jumpPhase * 0.14, 1.0 + jumpPhase * 0.25, 1.0 - jumpPhase * 0.14);
            shadowMesh.scale.setScalar(1.0 - jumpPhase * 0.38);
        } else {
            mascotGroup.position.y = Math.sin(elapsedTime * 2.8) * 0.08;
            mascotGroup.scale.set(1.0, 1.0, 1.0);
            shadowMesh.scale.setScalar(1.0);
        }

        const ghostOrbitRadius = 1.62;
        const ghostSpeed = 1.45;
        const ghostAngle = elapsedTime * ghostSpeed;
        ghostGroup.position.x = Math.cos(ghostAngle) * ghostOrbitRadius;
        ghostGroup.position.z = Math.sin(ghostAngle) * ghostOrbitRadius * 0.85;
        ghostGroup.position.y = 1.08 + Math.sin(elapsedTime * 3.4) * 0.22;
        ghostGroup.rotation.y = -ghostAngle + Math.PI / 2;

        ghostPointLight.position.copy(ghostGroup.position);
        ghostSkirt.rotation.z = Math.sin(elapsedTime * 6.5) * 0.22;

        const pupilTrackX = Math.max(-0.06, Math.min(0.06, currentRotY * 0.14));
        const pupilTrackY = Math.max(-0.06, Math.min(0.06, -currentRotX * 0.14));
        leftPupil.position.x = 0.03 + pupilTrackX;
        leftPupil.position.y = 0.02 + pupilTrackY;
        rightPupil.position.x = -0.03 + pupilTrackX;
        rightPupil.position.y = 0.02 + pupilTrackY;

        const pArr = particleGeo.attributes.position.array;
        for (let i = 1; i < pArr.length; i += 3) {
            pArr[i] += 0.005;
            if (pArr[i] > 2.0) pArr[i] = -1.2;
        }
        particleGeo.attributes.position.needsUpdate = true;

        renderer.render(scene, camera);
    }

    animate();
"""

html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <title>FoodLab 3D Mascot</title>
    <style>
        * {{ margin: 0; padding: 0; overflow: hidden; user-select: none; -webkit-user-select: none; touch-action: none; }}
        body, html {{ width: 100%; height: 100%; background: transparent !important; }}
        #canvas-container {{ width: 100%; height: 100%; position: absolute; top: 0; left: 0; }}
        canvas {{ display: block; width: 100% !important; height: 100% !important; background: transparent !important; }}
    </style>
</head>
<body>
<div id="canvas-container"></div>
<script>
{three_js}
</script>
<script>
{scene_js}
</script>
</body>
</html>
"""

dest = 'app/src/main/assets/mascot_3d/index.html'
with open(dest, 'w', encoding='utf-8') as f:
    f.write(html)

print("Updated app/src/main/assets/mascot_3d/index.html with bulletproof size detection & animation loop!")
