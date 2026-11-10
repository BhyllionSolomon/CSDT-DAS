// Shrinks a photo to at most 1920 px wide and turns it into a JPEG under about 2.5 MB.
export async function compressImage(file, maxWidth = 1920) {
    if (!file || !file.type.startsWith('image/')) throw new Error('Please choose a picture.')

    let bitmap
    try {
        bitmap = await createImageBitmap(file)
    } catch {
        throw new Error('This picture format is not supported. Please use a JPG, PNG or WebP picture.')
    }

    const scale = Math.min(1, maxWidth / bitmap.width)
    const width = Math.round(bitmap.width * scale)
    const height = Math.round(bitmap.height * scale)

    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, width, height)
    ctx.drawImage(bitmap, 0, 0, width, height)

    let quality = 0.85
    let blob = await toBlob(canvas, quality)
    while (blob && blob.size > 2.5 * 1024 * 1024 && quality > 0.45) {
        quality -= 0.1
        blob = await toBlob(canvas, quality)
    }
    if (!blob) throw new Error('This picture could not be prepared. Please try another one.')
    return blob
}

const toBlob = (canvas, quality) => new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', quality))